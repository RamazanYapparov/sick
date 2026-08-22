#!/usr/bin/env bash
set -euo pipefail

tool_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
dotnet_bin="$tool_dir/.dotnet/dotnet"
installer="$tool_dir/.cache/dotnet-install.sh"

if [[ ! -x "$dotnet_bin" ]]; then
  mkdir -p "$tool_dir/.cache" "$tool_dir/.dotnet"
  curl --fail --location --silent --show-error \
    https://dot.net/v1/dotnet-install.sh \
    --output "$installer"
  chmod +x "$installer"
  "$installer" --channel 10.0 --install-dir "$tool_dir/.dotnet" --no-path
fi

DOTNET_CLI_TELEMETRY_OPTOUT=1 \
DOTNET_SKIP_FIRST_TIME_EXPERIENCE=1 \
DOTNET_CLI_HOME="$tool_dir/.dotnet-home" \
NUGET_PACKAGES="$tool_dir/.nuget/packages" \
  "$dotnet_bin" restore "$tool_dir/SiqPackageGenerator.csproj"

echo "SIQ generator dependencies are ready."
