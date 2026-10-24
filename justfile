# Velocity + two Paper backends (docker/docker-compose.yml)

version := `sed -n 's/^version=//p' gradle.properties`
jar := "jar/build/libs/pv-addon-groups-" + version + ".jar"
compose := "docker compose -f docker/docker-compose.yml"
data := "docker/data"

# List recipes
default:
    @just --list

# Build the server/proxy addon jar
build:
    ./gradlew :jar:shadowJar

# Build and copy the addon jar into the proxy's plugins
deploy: build
    mkdir -p {{ data }}/velocity/plugins {{ data }}/server-1 {{ data }}/server-2
    cp {{ jar }} {{ data }}/velocity/plugins/pv-addon-groups.jar

# Deploy and start the proxy and both backends in the background
up: deploy
    {{ compose }} up -d

# Stop and remove the containers, keeping the server data
down:
    {{ compose }} down

# Redeploy and restart only the proxy, where the addon runs
restart: deploy
    {{ compose }} restart velocity

# Follow logs, optionally of one service (velocity, server-1, server-2)
logs service="":
    {{ compose }} logs -f {{ service }}

# Attach to a console (detach with Ctrl-P Ctrl-Q)
console service="velocity":
    {{ compose }} attach {{ service }}

# Remove the containers and all server data (worlds, configs, groups.json)
reset: down
    rm -rf {{ data }}
