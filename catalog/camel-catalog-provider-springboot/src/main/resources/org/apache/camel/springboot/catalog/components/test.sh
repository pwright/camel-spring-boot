# Requires https://stedolan.github.io/jq/

# Create a single json file for querying

jq -s '.' *.json > test.json

# Create a definition list of components:

cat test.json | jq -r '.[]|"\(.component.name):: \(.component.description)"' >test.adoc

# Create a csv (with | as separator), viewable from table.adoc

cat test.json | jq -r '.[]|"\(.component.name)| \(.component.description)| \(.component.artifactId)"' >test.csv