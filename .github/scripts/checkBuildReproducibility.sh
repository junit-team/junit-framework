#!/bin/bash -e

rm -f checksums-1.txt checksums-2.txt

SOURCE_DATE_EPOCH=$(date +%s)
export SOURCE_DATE_EPOCH

function calculate_checksums() {
    OUTPUT=$1

    ./gradlew \
        --configuration-cache \
        --no-build-cache \
        -Dorg.gradle.java.installations.auto-download=false \
        -Dscan.tag.Reproducibility \
        clean \
        assemble

    find . -name '*.jar' \
        | grep '/build/libs/' \
        | grep --invert-match 'javadoc' \
        | sort \
        | xargs sha512sum > "${OUTPUT}"
}


calculate_checksums checksums-1.txt
calculate_checksums checksums-2.txt

diff checksums-1.txt checksums-2.txt
