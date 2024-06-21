const express = require('express');
const app = express();
const axios = require('axios');
const path = require('path');
const mysql = require('mysql');
const PORT = process.env.PORT || 3000; // Set the port to listen on, or use 3000 as default
const ytdl = require('ytdl-core'); // For downloading YouTube videos
const fs = require('fs');
const sanitize = require('sanitize-filename'); // For sanitizing file names
const exec = require('child_process').exec;

// MySQL database connection
const connection = mysql.createConnection({
  host: 'database-test.cplloyu1pt27.us-east-1.rds.amazonaws.com',
  user: 'admin',
  password: 'Dolgormaa0',
  database: 'db'
});

connection.connect((err) => {
  if (err) {
    console.error('Error connecting to database:', err.stack);
    return;
  }
  console.log('Connected to database.');

  // Create users table if it doesn't exist
  connection.query(`
    CREATE TABLE IF NOT EXISTS users (
      user_id VARCHAR(255) PRIMARY KEY,
      password VARCHAR(255) NOT NULL
    )
  `, (error, results, fields) => {
    if (error) {
      console.error('Error creating table:', error);
    } else {
      console.log('Table users created successfully.');
    }
  });

  // Create youtube_urls table if it doesn't exist
  connection.query(`
    CREATE TABLE IF NOT EXISTS youtube_urls (
      id INT AUTO_INCREMENT PRIMARY KEY,
      user_id VARCHAR(255) NOT NULL,
      videoId VARCHAR(255) NOT NULL,
      title VARCHAR(255) NOT NULL,
      channelName VARCHAR(255) NOT NULL,
      url VARCHAR(255) NOT NULL,
      FOREIGN KEY (user_id) REFERENCES users(user_id)
    )
  `, (error, results, fields) => {
    if (error) {
      console.error('Error creating table:', error);
    } else {
      console.log('Table youtube_urls created successfully.');
    }
  });
});

// Middleware to parse JSON request bodies
app.use(express.json());

// Serve static files (HTML, CSS, JavaScript) from the same directory
app.use(express.static(__dirname));

// Route to handle user login
app.get('/login', (req, res) => {
  const userId = req.query.userId;
  const password = req.query.password;

  connection.query(
    'SELECT * FROM users WHERE user_id = ? AND password = ?',
    [userId, password],
    (error, results, fields) => {
      if (error) {
        console.error('Error executing query:', error);
        res.status(500).json({ error: 'Internal Server Error' });
        return;
      }

      if (results.length > 0) {
        res.json({ success: true });
      } else {
        res.json({ success: false });
      }
    }
  );
});

// Route to proxy requests to the YouTube Data API
app.get('/search', async (req, res) => {
  try {
    const apiKey = 'AIzaSyB1ayWMjAVtWYe_mgWxcK-wIzYk6Ugz-mE'; // Replace 'YOUR_YOUTUBE_API_KEY' with your actual YouTube API key
    const query = req.query.q; // Get the search query from the request query parameters
    const user_id = req.query.user_id; // Get the user_id from the request query parameters
    const url = `https://www.googleapis.com/youtube/v3/search?key=${apiKey}&q=${query}&maxResults=10&type=video&part=snippet`;
    console.log('Fetching data from YouTube API:', url);
    // Make a GET request to the YouTube API using axios
    const response = await axios.get(url);
    const data = response.data;

    console.log('YouTube API response:', data);

    // Get detailed information for each video
    const videos = await Promise.all(
      data.items.map(async item => {
        const videoId = item.id.videoId;
        const videoInfoUrl = `https://www.googleapis.com/youtube/v3/videos?part=snippet&id=${videoId}&key=${apiKey}`;
        const videoResponse = await axios.get(videoInfoUrl);
        const videoData = videoResponse.data;

        // Check if video data is available
        if (!videoId || !videoData.items || !videoData.items.length || !videoData.items[0].snippet) {
          console.log(`Skipping video ${videoId} due to missing data`);
          return null;
        }

        const title = videoData.items[0].snippet.title;
        const channelName = videoData.items[0].snippet.channelTitle;
        const thumbnailUrl = videoData.items[0].snippet.thumbnails.default.url; // Thumbnail URL

        // Check if video title, channel name, or thumbnail URL is missing
        if (!title || !channelName || !thumbnailUrl) {
          console.log(`Skipping video ${videoId} due to missing title, channel name, or thumbnail URL`);
          return null;
        }

        return {
          videoId: videoId,
          title: title,
          channelName: channelName,
          thumbnailUrl: thumbnailUrl
        };
      })
    );

    // Filter out null values
    const validVideos = videos.filter(video => video !== null);

    // Send the response back to the client
    res.json(validVideos);
  } catch (error) {
    console.error('Error:', error);
    res.status(500).json({ error: 'Internal Server Error' });
  }
});

// Function to download audio from YouTube
async function downloadAudio(videoId) {
    try {
        // Prepare file path and name
        const fileName = `${videoId}.mp3`;
        const filePath = path.join(__dirname, 'downloads', fileName);
        const tempFilePath = path.join(__dirname, 'downloads', `${videoId}.temp`);

        // Download audio from YouTube
        const audioStream = ytdl(`https://www.youtube.com/watch?v=${videoId}`, {
            quality: 'highestaudio',
            filter: 'audioonly',
        });

        // Create directory if it doesn't exist
        if (!fs.existsSync(path.join(__dirname, 'downloads'))) {
            fs.mkdirSync(path.join(__dirname, 'downloads'));
        }

        // Write to a temporary file
        const writeStream = fs.createWriteStream(tempFilePath);
        audioStream.pipe(writeStream);

        return new Promise((resolve, reject) => {
            audioStream.on('end', () => {
                console.log(`Downloading: ${videoId}`);

                // Convert to MP3 using FFmpeg
                const ffmpegCommand = `ffmpeg -i ${tempFilePath} -codec:a libmp3lame -qscale:a 2 ${filePath}`;
                exec(ffmpegCommand, (err, stdout, stderr) => {
                    if (err) {
                        console.error('Error converting audio:', err);
                        reject(err);
                        return;
                    }

                    console.log(`MP3 file downloaded: ${fileName}`);
                    fs.unlinkSync(tempFilePath); // Remove temporary file
                    resolve(fileName);
                });
            });

            audioStream.on('error', (err) => {
                console.error('Error downloading audio:', err);
                reject(err);
            });
        });
    } catch (error) {
        console.error('Error in downloadAudio:', error);
        throw error;
    }
}

// Route to save selected video information and download audio
app.get('/save', async (req, res) => {
  const userId = req.query.user_id;
  const videoId = req.query.videoId;
  const title = req.query.title;
  const channelName = req.query.channelName;

  console.log(`Saving video for userId=${userId}, videoId=${videoId}`);
  
  if (!userId || !videoId || !title || !channelName) {
    res.status(400).json({ error: 'Missing required parameters' });
    return;
  }

  try {
    // Check if the MP3 file already exists
    const fileName = `${videoId}.mp3`;
    const filePath = path.join(__dirname, 'downloads', fileName);

    if (fs.existsSync(filePath)) {
      console.log(`File ${fileName} already exists. Skipping download.`);
      // Insert into database
      connection.query(
        'INSERT INTO youtube_urls (user_id, videoId, title, channelName, url) VALUES (?, ?, ?, ?, ?)',
        [userId, videoId, title, channelName, `https://www.youtube.com/watch?v=${videoId}`],
        (error, results, fields) => {
          if (error) {
            console.error('Error inserting URL into database:', error);
            res.status(500).json({ error: 'Internal Server Error' });
          } else {
            console.log('Inserted URL into database:', `https://www.youtube.com/watch?v=${videoId}`);
            res.json({ success: true, fileName: fileName });
          }
        }
      );
      return;
    }

    // Insert into database
    connection.query(
      'INSERT INTO youtube_urls (user_id, videoId, title, channelName, url) VALUES (?, ?, ?, ?, ?)',
      [userId, videoId, title, channelName, `https://www.youtube.com/watch?v=${videoId}`],
      async (error, results, fields) => {
        if (error) {
          console.error('Error inserting URL into database:', error);
          res.status(500).json({ error: 'Internal Server Error' });
        } else {
          console.log('Inserted URL into database:', `https://www.youtube.com/watch?v=${videoId}`);
          
          // Download the audio
          try {
            const downloadedFileName = await downloadAudio(videoId);
            res.json({ success: true, fileName: downloadedFileName });
          } catch (downloadError) {
            console.error('Error downloading audio:', downloadError);
            res.status(500).json({ error: 'Error downloading audio' });
          }
        }
      }
    );

  } catch (error) {
    console.error('Error fetching video data from YouTube:', error);
    res.status(500).json({ error: 'Internal Server Error' });
  }
});


// Route to retrieve data from the database
app.get('/data', (req, res) => {
        console.log('Sending data from the database');
  connection.query('SELECT * FROM youtube_urls', (error, results, fields) => {
    if (error) {
      console.error('Error executing query:', error);
      res.status(500).json({ error: 'Internal Server Error' });
      return;
    }
    res.json(results);
  });
});

// Route to handle custom SQL queries
app.post('/query', (req, res) => {
  const query = req.body.query;

  connection.query(query, (error, results, fields) => {
    if (error) {
      console.error('Error executing query:', error);
      res.status(500).json({ error: 'Internal Server Error' });
      return;
    }

    res.json(results);
  });
});

// Route to serve MP3 files
app.get('/play/:videoId', (req, res) => {
  const videoId = req.params.videoId;
  const fileName = `${videoId}.mp3`; 
  const filePath = path.join(__dirname, 'downloads', fileName);
console.log(`Serving the mp3 file in response: ${fileName}`);
  // Check if the file exists
  fs.access(filePath, fs.constants.F_OK, (err) => {
    if (err) {
      console.error(`File ${fileName} does not exist`);
      res.status(404).send('File not found');
      return;
    }

    // Set the appropriate content type
    res.setHeader('Content-Type', 'audio/mpeg');

    // Stream the file to the client
    const fileStream = fs.createReadStream(filePath);
    fileStream.pipe(res);
  });
});

// Route to check if MP3 file exists
app.get('/check-mp3', (req, res) => {
  const videoId = req.query.videoId;
  const fileName = `${videoId}.mp3`;
  const filePath = path.join(__dirname, 'downloads', fileName);

  // Check if the file exists
  fs.access(filePath, fs.constants.F_OK, (err) => {
    if (err) {
      console.error(`MP3 file ${fileName} does not exist`);
      res.json({ exists: false });
    } else {
      console.log(`MP3 file ${fileName} exists`);
      res.json({ exists: true });
    }
  });
});

// Start the server
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});