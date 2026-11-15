#!/bin/bash -e

rm -f checksums-1.txt checksums-2.txt

function calculate_checksums() {
    local OUTPUT=$1
    local EXTRA_GRADLE_ARGS=("${@:2}")

    ./gradlew \
        --no-build-cache \
        -Dorg.gradle.java.installations.auto-download=false \
        -Dscan.tag.Reproducibility \
        "${EXTRA_GRADLE_ARGS[@]}" \
        clean \
        assemble

    find . -name '*.jar' \
        | grep '/build/libs/' \
        | grep --invert-match 'javadoc' \
        | sort \
        | xargs sha512sum > "${OUTPUT}"
}

function manifest_attribute() {
    unzip -c "${REFERENCE_JAR}" META-INF/MANIFEST.MF | jc --jar-manifest | jq --raw-output ".[0].$1"
}

calculate_checksums checksums-1.txt

# Like the release workflow, the second build takes its timestamp from a JAR manifest of the first
REFERENCE_JAR=$(ls junit-jupiter-api/build/libs/*.jar | grep --invert-match -e sources -e javadoc -e test-fixtures)
calculate_checksums checksums-2.txt -PsourceDateEpoch="$(manifest_attribute Build_Date) $(manifest_attribute Build_Time)"

diff checksums-1.txt checksums-2.txt
