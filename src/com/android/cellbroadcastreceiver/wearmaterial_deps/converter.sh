# copy source from go/wear-mat-lib

while read old_module_name; do
	# strip off extra stuff because it'll conflict with the real copy of wearmaterial elsewhere in the tree
	new_module_name=$(echo "$old_module_name" | sed 's/platform-cw-common-wearable-\(wearmaterial-.*\)-src/\1/g')

	# rename all mentions
	echo "renaming $old_module_name to $new_module_name"
	fd --type file --exclude modules.txt . | xargs sed -i "s/$old_module_name/$new_module_name/g"
done < modules.txt

echo "jetifying..."
for filepath in $(fd --no-ignore --extension .java . ); do
	echo "   ${filepath}"
	/usr/local/google/home/leezach/workspace/wear/tm-wear-kr3-dev/prebuilts/sdk/tools/jetifier/jetifier-standalone/bin/jetifier-standalone \
		-i $filepath \
		-o "$filepath"
done

echo "removing extra classes"
for filepath in $(fd --no-ignore --extension .java . ); do
	echo "   ${filepath}"
    	sed -i 's/import com.google.errorprone.annotations.ResultIgnorabilityUnspecified;//g' $filepath
	sed -i 's/@ResultIgnorabilityUnspecified//g' $filepath
done

# delete WearPreferenceActivity because it has dependencies on stuff outside of wearmaterial
# delete SettingsPreferenceActivity and MainPreferenceActivity because they depend on it
# delete placeholder because it's having weird build issues when I build the whole project, but not on its own
