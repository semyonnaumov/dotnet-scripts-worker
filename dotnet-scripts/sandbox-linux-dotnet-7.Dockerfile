FROM mcr.microsoft.com/dotnet/sdk:7.0.102-alpine3.17-amd64

WORKDIR /app

# Install dotnet-script runner
RUN dotnet tool install dotnet-script --tool-path /usr/bin

# Run script from the specified folder
ENTRYPOINT [ "dotnet-script", "/tmp/script/main.csx", "release"]