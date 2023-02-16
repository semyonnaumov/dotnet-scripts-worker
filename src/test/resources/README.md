Dummy files dummy.txt in dockercerts/ca and dockercerts/client are needed just to keep these folders 
in git and in the test classpath in order to use them as docker certificates storage when running 
integration tests on docker:dind container with testcontainers. This is a workaround, needed to
set up TLS verification when running tests. It's better to run docker:dind container which would 
expose an HTTP endpoint instead of HTTPS, e.g. at tcp://0.0.0.0:2375, but all the attempts for that failed for now.
Consider trying the following in the future:
docker run --privileged --entrypoint 'dockerd -H tcp://127.0.0.1:2376' docker:dind

Dummy file dummy.txt in tempfiles is needed to keep it in git and in the test classpath
for it to be used by JobFilesServiceImpl to save job files to it.
