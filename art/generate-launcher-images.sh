#!/bin/sh

# Source PNG file, relative to this script's location
SOURCE_IMAGE_PATH="../U.png"

if [ ! -f "$SOURCE_IMAGE_PATH" ]; then
	echo "Error: Source image $SOURCE_IMAGE_PATH not found."
	exit 1
fi

echo "Using source image: $SOURCE_IMAGE_PATH"

# Generate mipmap launcher icons (for adaptive icons)
for DENSITY in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
	case $DENSITY in
		mdpi) SIZE=48;;
		hdpi) SIZE=72;;
		xhdpi) SIZE=96;;
		xxhdpi) SIZE=144;;
		xxxhdpi) SIZE=192;;
	esac

	FOLDER=../app/src/main/res/mipmap-$DENSITY
	mkdir -p "$FOLDER"
	echo "Created folder: $FOLDER"

	for FILE_ALIAS in ic_launcher ic_launcher_round; do
		PNG_OUTPUT_PATH="$FOLDER/$FILE_ALIAS.png"
		echo "Generating $PNG_OUTPUT_PATH (size ${SIZE}x${SIZE})"
		
		# Use high-quality resize, do not reduce quality or add compression
		if ! convert "$SOURCE_IMAGE_PATH" -resize "${SIZE}x${SIZE}" -filter Lanczos "$PNG_OUTPUT_PATH"; then
			echo "Error: Failed to convert image for $PNG_OUTPUT_PATH."
			echo "Please ensure ImageMagick (convert command) is installed and accessible."
			exit 1
		fi
		
		# Optimize the generated PNG (lossless)
		if ! zopflipng -y "$PNG_OUTPUT_PATH" "$PNG_OUTPUT_PATH"; then
			echo "Warning: zopflipng optimization failed for $PNG_OUTPUT_PATH."
			echo "The unoptimized file will be used. Please ensure zopflipng is installed for optimization."
		fi
	done
done

# Generate drawable u_launcher.png for permission icon and other direct drawable references
DRAWABLE_FOLDER=../app/src/main/res/drawable
mkdir -p "$DRAWABLE_FOLDER"
DRAWABLE_ICON_PATH="$DRAWABLE_FOLDER/u_launcher.png"
DRAWABLE_SIZE=96 
echo "Generating $DRAWABLE_ICON_PATH (size ${DRAWABLE_SIZE}x${DRAWABLE_SIZE})"

if ! convert "$SOURCE_IMAGE_PATH" -resize "${DRAWABLE_SIZE}x${DRAWABLE_SIZE}" -filter Lanczos "$DRAWABLE_ICON_PATH"; then
	echo "Error: Failed to convert image for $DRAWABLE_ICON_PATH."
	echo "Please ensure ImageMagick (convert command) is installed and accessible."
	exit 1
fi

if ! zopflipng -y "$DRAWABLE_ICON_PATH" "$DRAWABLE_ICON_PATH"; then
	echo "Warning: zopflipng optimization failed for $DRAWABLE_ICON_PATH."
	echo "The unoptimized file will be used."
fi

echo "Launcher images generated successfully in ../app/src/main/res/mipmap-* folders."
echo "Drawable u_launcher.png generated in $DRAWABLE_FOLDER."
