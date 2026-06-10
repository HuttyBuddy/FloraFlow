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

// Define output directory
const outputDir = path.join(__dirname, 'enhanced');
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

// Screenshot titles mapping
const screenshotMetadata = {
  // Planner screen
  'Screenshot_20260530_091016.png': {
    title: 'DESIGN YOUR GARDEN',
    subtitle: 'Interactive 2D grids & companion checks'
  },
  // AR screen
  'Screenshot_20260530_090634.png': {
    title: 'SPATIAL HOLOGRAMS',
    subtitle: 'Project mature plants in real space'
  },
  // Library screen
  'Screenshot_20260530_104459.png': {
    title: 'BOTANICAL KNOWLEDGE',
    subtitle: 'Explore pH soil levels and seasonal care'
  },
  // Dashboard screen
  'Screenshot_20260529_215510.png': {
    title: 'GROWTH & WELL-BEING',
    subtitle: 'Track gardening sessions vs mood rating'
  },
  // Onboarding screens
  'Screenshot_20260529_152706.png': {
    title: 'WELCOME TO FLORAFLOW',
    subtitle: 'Coactive space planning and wellness'
  },
  'Screenshot_20260529_220719.png': {
    title: 'AI ADVISOR CHAT',
    subtitle: 'Consult Dr. Greenleaf on soil moisture'
  }
};

// Default metadata for unmapped screenshots
const defaultMetadata = {
  title: 'FLORAFLOW DESIGNER',
  subtitle: 'Therapeutic spaces, advisor, and AR'
};

// Scan the directory for raw screenshots
const files = fs.readdirSync(__dirname);
const screenshots = files.filter(f => f.startsWith('Screenshot_') && f.endsWith('.png'));

console.log(`Found ${screenshots.length} raw screenshots. Beginning enhancement process...`);

screenshots.forEach((file) => {
  const inputPath = path.join(__dirname, file);
  const outputPath = path.join(outputDir, file);

  const meta = screenshotMetadata[file] || defaultMetadata;
  const escapedTitle = meta.title.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  const escapedSubtitle = meta.subtitle.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

  console.log(`Processing: ${file} -> "${meta.title}"...`);

  // Read raw image as base64
  const base64Image = fs.readFileSync(inputPath).toString('base64');

  // Build the gorgeous SVG markup
  const svgMarkup = `
<svg width="1242" height="2208" viewBox="0 0 1242 2208" xmlns="http://www.w3.org/2000/svg">
  <!-- Gradient background -->
  <defs>
    <linearGradient id="bg-grad" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="#0E1E1B"/>
      <stop offset="50%" stop-color="#1B3A33"/>
      <stop offset="100%" stop-color="#2D544A"/>
    </linearGradient>
    
    <linearGradient id="phone-border-grad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#E2C4A2"/>
      <stop offset="100%" stop-color="#3A6C5E"/>
    </linearGradient>
    
    <filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">
      <feDropShadow dx="0" dy="24" stdDeviation="28" flood-color="#000000" flood-opacity="0.55"/>
    </filter>

    <clipPath id="screen-clip">
      <rect x="237" y="462" width="768" height="1588" rx="48" ry="48"/>
    </clipPath>
  </defs>

  <!-- Background -->
  <rect width="1242" height="2208" fill="url(#bg-grad)"/>

  <!-- Leaf Pattern details (decorative top/bottom corners) -->
  <circle cx="-100" cy="-100" r="400" fill="#3A6C5E" opacity="0.1" />
  <circle cx="1342" cy="2308" r="400" fill="#E2C4A2" opacity="0.06" />

  <!-- Marketing Text -->
  <text x="621" y="210" text-anchor="middle" font-family="system-ui, -apple-system, sans-serif" font-weight="900" font-size="72" fill="#F4F2EC" letter-spacing="3">${escapedTitle}</text>
  <text x="621" y="300" text-anchor="middle" font-family="system-ui, -apple-system, sans-serif" font-weight="600" font-size="34" fill="#ACCFC6" letter-spacing="1">${escapedSubtitle}</text>

  <!-- Phone Mockup Container with Drop Shadow -->
  <g filter="url(#shadow)">
    <!-- Outer phone shell -->
    <rect x="221" y="446" width="800" height="1620" rx="60" ry="60" fill="#0A1412" stroke="url(#phone-border-grad)" stroke-width="12"/>
    
    <!-- Screen content -->
    <image href="data:image/png;base64,${base64Image}" x="237" y="462" width="768" height="1588" clip-path="url(#screen-clip)"/>
    
    <!-- Inner screen border line to simulate glass depth -->
    <rect x="237" y="462" width="768" height="1588" rx="48" ry="48" fill="none" stroke="#254C42" stroke-width="3" opacity="0.6"/>

    <!-- Speaker / Notch -->
    <rect x="521" y="466" width="200" height="20" rx="10" ry="10" fill="#0A1412"/>
    <circle cx="541" cy="476" r="4" fill="#1B3A33"/>
  </g>
</svg>
`;

  // Convert SVG markup to PNG using svg2img
  const svgBuffer = Buffer.from(svgMarkup);
  svg2img(svgBuffer, { width: 1242, height: 2208, preserveAspectRatio: true }, (error, buffer) => {
    if (error) {
      console.error(`Error converting ${file}:`, error);
    } else {
      fs.writeFileSync(outputPath, buffer);
      console.log(`🎉 Generated enhanced screenshot: ${file}`);
    }
  });
});

console.log('Screenshot enhancement complete! Assets saved under play_store_assets/enhanced/');
