#!/usr/bin/env bash

# Get the directory of the script
script_dir=$(dirname "$0")

# Run the JavaScript and Dart scripts and save their outputs
node_output=$(node "$script_dir/js/qs.js")
kt_output=$(../gradlew -q :comparison:run --console=plain --warning-mode=none)

# Compare the outputs
if [ "$node_output" == "$kt_output" ]; then
    echo "The outputs are identical."
    exit 0
else
    echo "The outputs are different."
    echo "Differences:"
    diff <(echo "$node_output") <(echo "$kt_output")
    exit 1
fi
