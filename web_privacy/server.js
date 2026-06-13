const http = require('http');
const fs = require('fs');
const path = require('path');
const port = process.env.PORT || 8080;

const server = http.createServer((req, res) => {
  // Support both /privacy and root / routes
  if (req.url === '/privacy' || req.url === '/' || req.url === '/index.html') {
    fs.readFile(path.join(__dirname, 'public', 'index.html'), 'utf8', (err, content) => {
      if (err) {
        res.writeHead(500, { 'Content-Type': 'text/plain' });
        res.end('Internal Server Error');
      } else {
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.end(content);
      }
    });
  } else if (req.url === '/terms' || req.url === '/terms.html') {
    fs.readFile(path.join(__dirname, 'public', 'terms.html'), 'utf8', (err, content) => {
      if (err) {
        res.writeHead(500, { 'Content-Type': 'text/plain' });
        res.end('Internal Server Error');
      } else {
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.end(content);
      }
    });
  } else {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Not Found');
  }
});

server.listen(port, () => {
  console.log(`Server listening on port ${port}`);
});
