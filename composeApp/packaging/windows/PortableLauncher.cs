// The portable sick.exe: the whole app image (launcher, Java runtime, libVLC) rides
// inside this executable as a zip. The first start of a build extracts it to
// %LOCALAPPDATA%\sick\<payload id>; later starts reuse that folder. The app runs
// with %LOCALAPPDATA%\sick as its working directory, so its logs land there.
// Built by :composeApp:packagePortableExe with the .NET Framework 4 compiler
// that every Windows 10/11 ships, so the exe needs nothing installed to run.
using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Windows.Forms;

static class PortableLauncher
{
    const string CompleteMarker = ".complete";

    [STAThread]
    static int Main()
    {
        try
        {
            return Run();
        }
        catch (Exception e)
        {
            MessageBox.Show(e.ToString(), "sick could not start", MessageBoxButtons.OK, MessageBoxIcon.Error);
            return 1;
        }
    }

    static int Run()
    {
        Assembly self = Assembly.GetExecutingAssembly();
        // payload.txt: the payload's SHA-256 on the first line, the app launcher's file name on the second.
        string[] info;
        using (var reader = new StreamReader(self.GetManifestResourceStream("payload.txt")))
            info = reader.ReadToEnd().Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries);
        string id = info[0].Trim();
        string launcher = info[1].Trim();

        string home = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "sick");
        string appDir = Path.Combine(home, id);
        if (!File.Exists(Path.Combine(appDir, CompleteMarker)))
            Extract(self, home, appDir);
        RemoveOtherVersions(home, id);

        var start = new ProcessStartInfo(Path.Combine(appDir, launcher))
        {
            UseShellExecute = false,
            WorkingDirectory = home,
        };
        using (Process app = Process.Start(start))
        {
            app.WaitForExit();
            return app.ExitCode;
        }
    }

    // Extracts next to the target and renames it into place, so a crash or a second
    // instance never leaves a half-written folder that looks complete.
    static void Extract(Assembly self, string home, string appDir)
    {
        string staging = appDir + ".tmp-" + Process.GetCurrentProcess().Id;
        if (Directory.Exists(staging)) Directory.Delete(staging, true);
        using (Stream payload = self.GetManifestResourceStream("payload.zip"))
        using (var zip = new ZipArchive(payload, ZipArchiveMode.Read))
            zip.ExtractToDirectory(staging);
        File.WriteAllText(Path.Combine(staging, CompleteMarker), "");

        if (Directory.Exists(appDir)) Directory.Delete(appDir, true);
        try
        {
            Directory.Move(staging, appDir);
        }
        catch (IOException)
        {
            // Another instance finished the same extraction first.
            if (!File.Exists(Path.Combine(appDir, CompleteMarker))) throw;
            Directory.Delete(staging, true);
        }
    }

    // Folders of earlier builds. A folder still in use stays; its marker goes first,
    // so a partly deleted folder is extracted again if that build is started later.
    static void RemoveOtherVersions(string home, string id)
    {
        foreach (string dir in Directory.GetDirectories(home))
        {
            string name = Path.GetFileName(dir);
            if (name == id || name == "logs") continue;
            try
            {
                string marker = Path.Combine(dir, CompleteMarker);
                if (File.Exists(marker)) File.Delete(marker);
                Directory.Delete(dir, true);
            }
            catch (IOException) { }
            catch (UnauthorizedAccessException) { }
        }
    }
}
