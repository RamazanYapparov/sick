using System.Text.Json;
using System.Text.Json.Serialization;
using SIPackages;
using SIPackages.Core;

return await SiqTool.RunAsync(args);

internal static class SiqTool
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true,
        ReadCommentHandling = JsonCommentHandling.Skip,
        UnmappedMemberHandling = JsonUnmappedMemberHandling.Disallow,
    };

    public static async Task<int> RunAsync(string[] args)
    {
        try
        {
            if (args.Length == 0 || args[0] is "help" or "--help" or "-h")
            {
                PrintHelp();
                return 0;
            }

            return args[0] switch
            {
                "generate" => await GenerateAsync(args[1..]),
                "validate" => Validate(args[1..]),
                "list-media" => ListMedia(args[1..]),
                _ => throw new UsageException($"Unknown command: {args[0]}")
            };
        }
        catch (UsageException exception)
        {
            Console.Error.WriteLine($"Error: {exception.Message}");
            Console.Error.WriteLine("Run 'siq-tool help' for usage.");
            return 2;
        }
        catch (Exception exception)
        {
            Console.Error.WriteLine($"Error: {exception.Message}");
            return 1;
        }
    }

    private static async Task<int> GenerateAsync(string[] args)
    {
        var options = ParseOptions(args, requireInput: true);
        var recipePath = Path.GetFullPath(options.Input!);
        var toolDirectory = AppContext.BaseDirectory;
        var mediaRoot = Path.GetFullPath(options.MediaRoot ?? FindToolPath(toolDirectory, "media"));
        var defaultOutputDirectory = FindToolPath(toolDirectory, "artifacts");

        if (!File.Exists(recipePath))
        {
            throw new UsageException($"Recipe does not exist: {recipePath}");
        }

        var recipeJson = await File.ReadAllTextAsync(recipePath);
        var recipe = JsonSerializer.Deserialize<PackageRecipe>(recipeJson, JsonOptions)
            ?? throw new UsageException("Recipe is empty.");
        RecipeValidator.Validate(recipe, mediaRoot);

        var outputPath = Path.GetFullPath(
            options.Output ?? Path.Combine(defaultOutputDirectory, $"{SafeFileName(recipe.Name)}.siq"));

        if (File.Exists(outputPath) && !options.Force)
        {
            throw new UsageException($"Output already exists: {outputPath}. Pass --force to replace it.");
        }

        Directory.CreateDirectory(Path.GetDirectoryName(outputPath)!);
        var temporaryPath = $"{outputPath}.{Guid.NewGuid():N}.tmp";

        try
        {
            await BuildPackageAsync(recipe, mediaRoot, temporaryPath);
            ValidatePackage(temporaryPath);
            File.Move(temporaryPath, outputPath, overwrite: options.Force);
        }
        finally
        {
            if (File.Exists(temporaryPath))
            {
                File.Delete(temporaryPath);
            }
        }

        Console.WriteLine($"Created: {outputPath}");
        PrintPackageSummary(outputPath);
        return 0;
    }

    private static async Task BuildPackageAsync(PackageRecipe recipe, string mediaRoot, string outputPath)
    {
        await using var output = new FileStream(outputPath, FileMode.CreateNew, FileAccess.ReadWrite, FileShare.None);
        using var document = SIDocument.Create(recipe.Name, recipe.Author, output, leaveStreamOpen: true);
        var package = document.Package;

        package.Date = recipe.Date ?? package.Date;
        package.ID = recipe.Id ?? package.ID;
        package.Difficulty = recipe.Difficulty;
        package.Language = recipe.Language;
        package.Publisher = recipe.Publisher ?? "";
        package.Tags.AddRange(recipe.Tags);

        var addedMedia = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);

        foreach (var roundRecipe in recipe.Rounds)
        {
            var round = new Round
            {
                Name = roundRecipe.Name,
                Type = NormalizeRoundType(roundRecipe.Type),
            };
            package.Rounds.Add(round);

            foreach (var themeRecipe in roundRecipe.Themes)
            {
                var theme = new Theme { Name = themeRecipe.Name };
                round.Themes.Add(theme);

                foreach (var questionRecipe in themeRecipe.Questions)
                {
                    var question = new Question
                    {
                        Price = questionRecipe.Price,
                        TypeName = NormalizeQuestionType(questionRecipe.Type),
                    };

                    question.Parameters[QuestionParameterNames.Question] = new StepParameter
                    {
                        Type = StepParameterTypes.Content,
                        ContentValue = await BuildContentAsync(
                            questionRecipe.Content,
                            document,
                            mediaRoot,
                            addedMedia),
                    };

                    if (questionRecipe.AnswerContent.Count > 0)
                    {
                        question.Parameters[QuestionParameterNames.Answer] = new StepParameter
                        {
                            Type = StepParameterTypes.Content,
                            ContentValue = await BuildContentAsync(
                                questionRecipe.AnswerContent,
                                document,
                                mediaRoot,
                                addedMedia),
                        };
                    }

                    foreach (var (name, parameterRecipe) in questionRecipe.Parameters)
                    {
                        question.Parameters[name] = await BuildParameterAsync(
                            parameterRecipe,
                            document,
                            mediaRoot,
                            addedMedia);
                    }

                    question.Right.AddRange(questionRecipe.Right);
                    question.Wrong.AddRange(questionRecipe.Wrong);
                    theme.Questions.Add(question);
                }
            }
        }

        document.Save();
    }

    private static async Task<StepParameter> BuildParameterAsync(
        ParameterRecipe recipe,
        SIDocument document,
        string mediaRoot,
        IDictionary<string, string> addedMedia)
    {
        switch (recipe.Type.Trim().ToLowerInvariant())
        {
            case "simple":
                return new StepParameter
                {
                    Type = StepParameterTypes.Simple,
                    SimpleValue = recipe.Value!,
                    IsRef = recipe.IsRef,
                };

            case "content":
                return new StepParameter
                {
                    Type = StepParameterTypes.Content,
                    ContentValue = await BuildContentAsync(recipe.Content, document, mediaRoot, addedMedia),
                };

            case "group":
                var group = new StepParameters();
                foreach (var (name, childRecipe) in recipe.Parameters)
                {
                    group[name] = await BuildParameterAsync(childRecipe, document, mediaRoot, addedMedia);
                }

                return new StepParameter
                {
                    Type = StepParameterTypes.Group,
                    GroupValue = group,
                };

            case "numberset":
                return new StepParameter
                {
                    Type = StepParameterTypes.NumberSet,
                    NumberSetValue = new NumberSet
                    {
                        Minimum = recipe.Minimum!.Value,
                        Maximum = recipe.Maximum!.Value,
                        Step = recipe.Step,
                    },
                };

            default:
                throw new UsageException($"Unsupported parameter type: {recipe.Type}");
        }
    }

    private static async Task<List<ContentItem>> BuildContentAsync(
        IReadOnlyList<ContentRecipe> recipes,
        SIDocument document,
        string mediaRoot,
        IDictionary<string, string> addedMedia)
    {
        var content = new List<ContentItem>(recipes.Count);

        foreach (var recipe in recipes)
        {
            var type = NormalizeContentType(recipe.Type);
            var item = new ContentItem
            {
                Type = type,
                WaitForFinish = recipe.WaitForFinish,
            };

            if (recipe.DurationSeconds is not null)
            {
                item.Duration = TimeSpan.FromSeconds(recipe.DurationSeconds.Value);
            }

            if (!string.IsNullOrWhiteSpace(recipe.Placement))
            {
                item.Placement = recipe.Placement;
            }

            if (type == ContentTypes.Text)
            {
                item.Value = recipe.Text!;
            }
            else if (recipe.File is not null)
            {
                var sourcePath = ResolveMediaPath(mediaRoot, recipe.File);
                var packageName = recipe.PackageName ?? Path.GetFileName(sourcePath);
                var collection = GetCollection(document, type);
                var mediaKey = $"{type}:{packageName}";

                if (addedMedia.TryGetValue(mediaKey, out var existingSource))
                {
                    if (!string.Equals(existingSource, sourcePath, StringComparison.Ordinal))
                    {
                        throw new UsageException(
                            $"Two files map to the same package name '{packageName}' for type '{type}'. " +
                            "Set a unique packageName.");
                    }
                }
                else
                {
                    await using var source = File.OpenRead(sourcePath);
                    await collection.AddFileAsync(packageName, source);
                    addedMedia[mediaKey] = sourcePath;
                }

                item.Value = packageName;
                item.IsRef = true;
            }
            else
            {
                item.Value = recipe.Url!;
                item.IsRef = false;
            }

            content.Add(item);
        }

        return content;
    }

    private static int Validate(string[] args)
    {
        if (args.Length != 1)
        {
            throw new UsageException("validate requires exactly one .siq path.");
        }

        var path = Path.GetFullPath(args[0]);
        ValidatePackage(path);
        Console.WriteLine($"Valid SIPackages document: {path}");
        PrintPackageSummary(path);
        return 0;
    }

    private static void ValidatePackage(string path)
    {
        if (!File.Exists(path))
        {
            throw new UsageException($"Package does not exist: {path}");
        }

        using var stream = File.OpenRead(path);
        using var document = SIDocument.Load(stream);

        if (string.IsNullOrWhiteSpace(document.Package.Name))
        {
            throw new InvalidDataException("Generated package has no name.");
        }

        if (document.Package.Rounds.Count == 0)
        {
            throw new InvalidDataException("Generated package has no rounds.");
        }
    }

    private static void PrintPackageSummary(string path)
    {
        using var stream = File.OpenRead(path);
        using var document = SIDocument.Load(stream);
        var themes = document.Package.Rounds.Sum(round => round.Themes.Count);
        var questions = document.Package.Rounds.Sum(round => round.Themes.Sum(theme => theme.Questions.Count));

        Console.WriteLine(
            $"Package '{document.Package.Name}': " +
            $"{document.Package.Rounds.Count} round(s), {themes} theme(s), {questions} question(s), " +
            $"media {document.Images.Count}/{document.Audio.Count}/{document.Video.Count}/{document.Html.Count} " +
            "(image/audio/video/html).");
    }

    private static int ListMedia(string[] args)
    {
        var root = Path.GetFullPath(args.Length switch
        {
            0 => FindToolPath(AppContext.BaseDirectory, "media"),
            1 => args[0],
            _ => throw new UsageException("list-media accepts zero or one media directory."),
        });

        if (!Directory.Exists(root))
        {
            throw new UsageException($"Media directory does not exist: {root}");
        }

        var files = Directory.EnumerateFiles(root, "*", SearchOption.AllDirectories)
            .Where(path => !string.Equals(Path.GetFileName(path), "README.md", StringComparison.OrdinalIgnoreCase))
            .Order(StringComparer.OrdinalIgnoreCase)
            .ToArray();

        foreach (var file in files)
        {
            Console.WriteLine(Path.GetRelativePath(root, file));
        }

        Console.WriteLine($"{files.Length} media file(s) in {root}");
        return 0;
    }

    private static CliOptions ParseOptions(string[] args, bool requireInput)
    {
        string? input = null;
        string? output = null;
        string? mediaRoot = null;
        var force = false;

        for (var index = 0; index < args.Length; index++)
        {
            switch (args[index])
            {
                case "--output" or "-o":
                    output = NextValue(args, ref index, "--output");
                    break;
                case "--media-root":
                    mediaRoot = NextValue(args, ref index, "--media-root");
                    break;
                case "--force" or "-f":
                    force = true;
                    break;
                default:
                    if (args[index].StartsWith('-'))
                    {
                        throw new UsageException($"Unknown option: {args[index]}");
                    }

                    if (input is not null)
                    {
                        throw new UsageException($"Unexpected argument: {args[index]}");
                    }

                    input = args[index];
                    break;
            }
        }

        if (requireInput && input is null)
        {
            throw new UsageException("generate requires a recipe JSON path.");
        }

        return new CliOptions(input, output, mediaRoot, force);
    }

    private static string NextValue(string[] args, ref int index, string option)
    {
        if (++index >= args.Length)
        {
            throw new UsageException($"{option} requires a value.");
        }

        return args[index];
    }

    private static string ResolveMediaPath(string mediaRoot, string relativePath)
    {
        if (Path.IsPathRooted(relativePath))
        {
            throw new UsageException($"Media path must be relative to media root: {relativePath}");
        }

        var root = Path.GetFullPath(mediaRoot).TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar;
        var resolved = Path.GetFullPath(Path.Combine(root, relativePath));

        if (!resolved.StartsWith(root, StringComparison.Ordinal))
        {
            throw new UsageException($"Media path escapes media root: {relativePath}");
        }

        if (!File.Exists(resolved))
        {
            throw new UsageException($"Media file does not exist: {resolved}");
        }

        return resolved;
    }

    private static DataCollection GetCollection(SIDocument document, string type) => type switch
    {
        ContentTypes.Image => document.Images,
        ContentTypes.Audio => document.Audio,
        ContentTypes.Video => document.Video,
        ContentTypes.Html => document.Html,
        _ => throw new UsageException($"Content type '{type}' cannot contain a file."),
    };

    private static string NormalizeContentType(string type) => type.Trim().ToLowerInvariant() switch
    {
        "text" => ContentTypes.Text,
        "image" => ContentTypes.Image,
        "audio" => ContentTypes.Audio,
        "video" => ContentTypes.Video,
        "html" => ContentTypes.Html,
        _ => throw new UsageException($"Unsupported content type: {type}"),
    };

    private static string NormalizeRoundType(string type) => type.Trim() switch
    {
        "standard" or "standart" => RoundTypes.Standart,
        "table" => RoundTypes.Table,
        "final" => RoundTypes.Final,
        "themeList" => RoundTypes.ThemeList,
        _ => throw new UsageException($"Unsupported round type: {type}"),
    };

    private static string NormalizeQuestionType(string? type) => type?.Trim() switch
    {
        null or "" or "default" => QuestionTypes.Default,
        var value => value,
    };

    private static string SafeFileName(string value)
    {
        var invalid = Path.GetInvalidFileNameChars();
        var result = new string(value.Select(character => invalid.Contains(character) ? '-' : character).ToArray()).Trim();
        return string.IsNullOrWhiteSpace(result) ? "package" : result;
    }

    private static string FindToolPath(string baseDirectory, string child)
    {
        var directory = new DirectoryInfo(baseDirectory);

        while (directory is not null)
        {
            if (File.Exists(Path.Combine(directory.FullName, "SiqPackageGenerator.csproj")))
            {
                return Path.Combine(directory.FullName, child);
            }

            directory = directory.Parent;
        }

        throw new InvalidOperationException("Cannot locate SIQ generator directory.");
    }

    private static void PrintHelp() => Console.WriteLine(
        """
        SIQ package generator based on the official SIPackages library.

        Commands:
          siq-tool generate <recipe.json> [--media-root <dir>] [-o <file.siq>] [--force]
          siq-tool validate <file.siq>
          siq-tool list-media [media-dir]
          siq-tool help

        Defaults:
          media root  ./media
          output      ./artifacts/<package name>.siq
        """);

    private sealed record CliOptions(string? Input, string? Output, string? MediaRoot, bool Force);
}

internal static class RecipeValidator
{
    public static void Validate(PackageRecipe recipe, string mediaRoot)
    {
        Required(recipe.Name, "name");
        Required(recipe.Author, "author");

        if (recipe.Difficulty is < 0 or > 10)
        {
            throw new UsageException("difficulty must be between 0 and 10.");
        }

        if (recipe.Rounds.Count == 0)
        {
            throw new UsageException("Recipe must contain at least one round.");
        }

        for (var roundIndex = 0; roundIndex < recipe.Rounds.Count; roundIndex++)
        {
            var round = recipe.Rounds[roundIndex];
            Required(round.Name, $"rounds[{roundIndex}].name");

            if (round.Themes.Count == 0)
            {
                throw new UsageException($"rounds[{roundIndex}] must contain at least one theme.");
            }

            for (var themeIndex = 0; themeIndex < round.Themes.Count; themeIndex++)
            {
                var theme = round.Themes[themeIndex];
                Required(theme.Name, $"rounds[{roundIndex}].themes[{themeIndex}].name");

                if (theme.Questions.Count == 0)
                {
                    throw new UsageException(
                        $"rounds[{roundIndex}].themes[{themeIndex}] must contain at least one question.");
                }

                for (var questionIndex = 0; questionIndex < theme.Questions.Count; questionIndex++)
                {
                    var question = theme.Questions[questionIndex];
                    var path = $"rounds[{roundIndex}].themes[{themeIndex}].questions[{questionIndex}]";

                    if (question.Price < 0)
                    {
                        throw new UsageException($"{path}.price must not be negative.");
                    }

                    if (question.Content.Count == 0)
                    {
                        throw new UsageException($"{path}.content must not be empty.");
                    }

                    if (question.Right.Count == 0)
                    {
                        throw new UsageException($"{path}.right must contain at least one answer.");
                    }

                    ValidateContent(question.Content, $"{path}.content", mediaRoot);
                    ValidateContent(question.AnswerContent, $"{path}.answerContent", mediaRoot);

                    foreach (var (name, parameter) in question.Parameters)
                    {
                        if (name is QuestionParameterNames.Question or QuestionParameterNames.Answer)
                        {
                            throw new UsageException(
                                $"{path}.parameters.{name} is reserved; use content or answerContent instead.");
                        }

                        Required(name, $"{path}.parameters key");
                        ValidateParameter(parameter, $"{path}.parameters.{name}", mediaRoot, depth: 0);
                    }
                }
            }
        }
    }

    private static void ValidateParameter(ParameterRecipe parameter, string path, string mediaRoot, int depth)
    {
        if (depth >= 10)
        {
            throw new UsageException($"{path} exceeds the maximum parameter nesting depth of 10.");
        }

        Required(parameter.Type, $"{path}.type");

        switch (parameter.Type.Trim().ToLowerInvariant())
        {
            case "simple":
                if (parameter.Value is null)
                {
                    throw new UsageException($"{path}.value is required for a simple parameter.");
                }
                break;

            case "content":
                if (parameter.Content.Count == 0)
                {
                    throw new UsageException($"{path}.content must not be empty.");
                }
                ValidateContent(parameter.Content, $"{path}.content", mediaRoot);
                break;

            case "group":
                if (parameter.Parameters.Count == 0)
                {
                    throw new UsageException($"{path}.parameters must not be empty.");
                }

                foreach (var (name, child) in parameter.Parameters)
                {
                    Required(name, $"{path}.parameters key");
                    ValidateParameter(child, $"{path}.parameters.{name}", mediaRoot, depth + 1);
                }
                break;

            case "numberset":
                if (parameter.Minimum is null || parameter.Maximum is null)
                {
                    throw new UsageException($"{path} requires minimum and maximum.");
                }

                if (parameter.Minimum > parameter.Maximum || parameter.Step < 0)
                {
                    throw new UsageException($"{path} has an invalid number set range.");
                }
                break;

            default:
                throw new UsageException($"{path}.type is unsupported: {parameter.Type}");
        }
    }

    private static void ValidateContent(IReadOnlyList<ContentRecipe> content, string path, string mediaRoot)
    {
        for (var index = 0; index < content.Count; index++)
        {
            var item = content[index];
            var itemPath = $"{path}[{index}]";
            Required(item.Type, $"{itemPath}.type");

            if (item.Type.Trim().ToLowerInvariant() is not ("text" or "image" or "audio" or "video" or "html"))
            {
                throw new UsageException($"{itemPath}.type is unsupported: {item.Type}");
            }

            if (string.Equals(item.Type, "text", StringComparison.OrdinalIgnoreCase))
            {
                if (item.Text is null || item.File is not null || item.Url is not null)
                {
                    throw new UsageException($"{itemPath} text content requires only 'text'.");
                }
            }
            else
            {
                var sourceCount = (item.File is null ? 0 : 1) + (item.Url is null ? 0 : 1);
                if (sourceCount != 1 || item.Text is not null)
                {
                    throw new UsageException($"{itemPath} media content requires exactly one of 'file' or 'url'.");
                }

                if (item.File is not null)
                {
                    ValidateRelativeMediaPath(mediaRoot, item.File, itemPath);
                }

                if (item.Url is not null &&
                    (!Uri.TryCreate(item.Url, UriKind.Absolute, out var uri) || uri.Scheme is not ("http" or "https")))
                {
                    throw new UsageException($"{itemPath}.url must be an absolute HTTP(S) URL.");
                }
            }

            if (item.PackageName is not null &&
                (string.IsNullOrWhiteSpace(item.PackageName) ||
                 Path.GetFileName(item.PackageName) != item.PackageName ||
                 item.PackageName is "." or ".."))
            {
                throw new UsageException($"{itemPath}.packageName must be a plain file name.");
            }

            if (item.Placement is not null && item.Placement is not ("screen" or "replic" or "background"))
            {
                throw new UsageException($"{itemPath}.placement is unsupported: {item.Placement}");
            }

            if (item.DurationSeconds < 0)
            {
                throw new UsageException($"{itemPath}.durationSeconds must not be negative.");
            }
        }
    }

    private static void ValidateRelativeMediaPath(string mediaRoot, string relativePath, string itemPath)
    {
        if (Path.IsPathRooted(relativePath))
        {
            throw new UsageException($"{itemPath}.file must be relative to media root.");
        }

        var root = Path.GetFullPath(mediaRoot).TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar;
        var resolved = Path.GetFullPath(Path.Combine(root, relativePath));
        if (!resolved.StartsWith(root, StringComparison.Ordinal))
        {
            throw new UsageException($"{itemPath}.file escapes media root.");
        }
    }

    private static void Required(string? value, string path)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            throw new UsageException($"{path} is required.");
        }
    }
}

internal sealed class PackageRecipe
{
    [JsonPropertyName("$schema")]
    public string? Schema { get; init; }
    public string Name { get; init; } = "";
    public string Author { get; init; } = "";
    public string? Id { get; init; }
    public string? Date { get; init; }
    public int Difficulty { get; init; } = 5;
    public string Language { get; init; } = "ru";
    public string? Publisher { get; init; }
    public List<string> Tags { get; init; } = [];
    public List<RoundRecipe> Rounds { get; init; } = [];
}

internal sealed class RoundRecipe
{
    public string Name { get; init; } = "";
    public string Type { get; init; } = "standard";
    public List<ThemeRecipe> Themes { get; init; } = [];
}

internal sealed class ThemeRecipe
{
    public string Name { get; init; } = "";
    public List<QuestionRecipe> Questions { get; init; } = [];
}

internal sealed class QuestionRecipe
{
    public int Price { get; init; }
    public string? Type { get; init; }
    public List<ContentRecipe> Content { get; init; } = [];
    public List<ContentRecipe> AnswerContent { get; init; } = [];
    public Dictionary<string, ParameterRecipe> Parameters { get; init; } = [];
    public List<string> Right { get; init; } = [];
    public List<string> Wrong { get; init; } = [];
}

internal sealed class ParameterRecipe
{
    public string Type { get; init; } = "simple";
    public string? Value { get; init; }
    public bool IsRef { get; init; }
    public List<ContentRecipe> Content { get; init; } = [];
    public Dictionary<string, ParameterRecipe> Parameters { get; init; } = [];
    public int? Minimum { get; init; }
    public int? Maximum { get; init; }
    public int Step { get; init; }
}

internal sealed class ContentRecipe
{
    public string Type { get; init; } = "text";
    public string? Text { get; init; }
    public string? File { get; init; }
    public string? Url { get; init; }
    public string? PackageName { get; init; }
    public string? Placement { get; init; }
    public double? DurationSeconds { get; init; }
    public bool WaitForFinish { get; init; } = true;
}

internal sealed class UsageException(string message) : Exception(message);
