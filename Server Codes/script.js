document.getElementById("loginForm").addEventListener("submit", function(event) {
  event.preventDefault();
  var userId = document.getElementById('userId').value;
  var password = document.getElementById('password').value;
  var url = '/login?userId=' + encodeURIComponent(userId) + '&password=' + encodeURIComponent(password);

  fetch(url)
      .then(response => {
          if (!response.ok) {
              throw new Error('Network response was not ok');
          }
          return response.json();
      })
      .then(data => {
          if (data.success) {
              document.getElementById('loginContainer').style.display = 'none';
              document.getElementById('searchContainer').style.display = 'block';
              document.getElementById('saveSelected').style.display = 'block';
              localStorage.setItem('userId', userId);
          } else {
              document.getElementById('loginError').innerText = 'Invalid user ID or password';
              document.getElementById('loginError').style.display = 'block';
          }
      })
      .catch(error => console.error('Error:', error));
});

async function searchYouTube() {
  const query = document.getElementById('searchQuery').value;
  const userId = localStorage.getItem('userId');
  const resultsContainer = document.getElementById('resultsContainer');
  const saveButton = document.getElementById('saveSelected');

  if (!query || !userId) {
    alert('Please enter a search query and make sure you are logged in.');
    return;
  }

  try {
    const response = await fetch(`/search?q=${encodeURIComponent(query)}&user_id=${encodeURIComponent(userId)}`);
    const videos = await response.json();
    displayResults(videos);

    if (videos.length > 0) {
      saveButton.style.display = 'block';
    } else {
      saveButton.style.display = 'none';
    }
  } catch (error) {
    console.error('Error:', error);
    alert('Failed to fetch YouTube videos.');
  }
}

function displayResults(data) {
  var resultsContainer = document.getElementById('resultsContainer');
  resultsContainer.innerHTML = ''; // Clear previous results

  data.forEach(item => {
    var videoElement = document.createElement('div');
    videoElement.classList.add('video');

    var thumbnailElement = document.createElement('img');
    thumbnailElement.src = item.thumbnailUrl;
    thumbnailElement.alt = "Thumbnail";
    thumbnailElement.classList.add('thumbnail');

    var infoElement = document.createElement('div');
    infoElement.classList.add('info');

    var titleElement = document.createElement('p');
    titleElement.classList.add('title');
    titleElement.textContent = "Title: " + item.title;

    var channelElement = document.createElement('p');
    channelElement.classList.add('channelName');
    channelElement.textContent = "Channel: " + item.channelName;

    var checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.value = item.videoId;

    infoElement.appendChild(titleElement);
    infoElement.appendChild(channelElement);
    infoElement.appendChild(checkbox);

    videoElement.appendChild(thumbnailElement);
    videoElement.appendChild(infoElement);

    resultsContainer.appendChild(videoElement);
  });
}
function saveSelected() {
  const selectedVideos = document.querySelectorAll('input[type="checkbox"]:checked');
  const userId = localStorage.getItem('userId');
  const savedTitles = [];

  selectedVideos.forEach(video => {
    const videoId = video.value;
    const title = video.parentElement.querySelector('.title').textContent.replace('Title: ', '');
    const channelName = video.parentElement.querySelector('.channelName').textContent.replace('Channel: ', '');
    const url = `/save?videoId=${encodeURIComponent(videoId)}&user_id=${encodeURIComponent(userId)}&title=${encodeURIComponent(title)}&channelName=${encodeURIComponent(channelName)}`;

    fetch(url)
      .then(response => {
        if (!response.ok) {
          throw new Error('Network response was not ok');
        }
        return response.json();
      })
      .then(data => {
        if (data.success) {
          savedTitles.push(title);
        }
        if (savedTitles.length === selectedVideos.length) {
          window.alert('Videos saved successfully:\n' + savedTitles.join('\n'));
        }
      })
      .catch(error => console.error('Error:', error));
  });
}