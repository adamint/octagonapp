#!/usr/bin/env bash

build() {
    project="$1"
    echo "Building $project .."
    cd ./${project}
    ./gradlew build
    cd ../
    docker build --tag "$project" --build-arg dir="$project" -f "./$project/Dockerfile" .
}

projects=("octagon-source-update" "octagonweb" "octagonapp")

for project in ${projects[@]} ; do
    build ${project}
done
