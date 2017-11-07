let path = require('path');
let uglify = require('uglifyjs-webpack-plugin')

const DIR = path.resolve(__dirname, 'public/jsx')
const OUT_DIR = path.resolve(__dirname, 'public/target')

module.exports = {
  entry: path.join(DIR, "JournalApp.jsx"),
  output: {
    path: OUT_DIR,
    filename: "JournalBundle.js"
  },
  plugins: [new uglify()],
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
