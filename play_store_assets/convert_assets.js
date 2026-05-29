const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Ensure dependencies are installed
try {
  require.resolve('svg2img');
} catch (e) {
  console.log('Installing lightweight svg2img package for conversion...');
  execSync('npm install --no-save svg2img', { stdio: 'inherit', cwd: __dirname });
}

const svg2img = require('svg2img');

const assets = [
  {
    input: 'app_icon_512.svg',
    output: 'app_icon_512.png',
    options: { width: 512, height: 512, preserveAspectRatio: true }
  },
  {
    input: 'feature_graphic_1024_500.svg',
    output: 'feature_graphic_1024_500.png',
    options: { width: 1024, height: 500, preserveAspectRatio: true }
  }
];

assets.forEach((asset) => {
  const inputPath = path.join(__dirname, asset.input);
  const outputPath = path.join(__dirname, asset.output);

  if (!fs.existsSync(inputPath)) {
    console.error(`Error: Input file ${asset.input} does not exist at ${inputPath}`);
    return;
  }

  console.log(`Converting ${asset.input} to high-resolution PNG at ${asset.options.width}x${asset.options.options || asset.options.height}...`);
  
  svg2img(inputPath, asset.options, (error, buffer) => {
    if (error) {
      console.error(`Error converting ${asset.input}:`, error);
    } else {
      fs.writeFileSync(outputPath, buffer);
      console.log(`🎉 Successfully generated: ${asset.output}`);
    }
  });
});
