docker run --name runner-"$2" -v \
  "/Users/snaumov/Desktop/Root/Projects/Backend/dotnet-scripts-worker/dotnet-scripts":/tmp/script:ro sandbox:"$1"
