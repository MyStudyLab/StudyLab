let path = require('path');

const DIR = path.resolve(__dirname, 'public/jsx')
const OUT_DIR = path.resolve(__dirname, 'public/target')

module.exports = {
  entry: path.join(DIR, "JournalApp.jsx"),
  output: {
    path: OUT_DIR,
    filename: "JournalBundle.js"
  },
  module: {

    rules: [
      // Files ending in .jsx are loaded with babel
      {
        test: /\.jsx$/,
        include: DIR,
        use: {
          loader: 'babel-loader'
        }
      }
    ]

  }
}
