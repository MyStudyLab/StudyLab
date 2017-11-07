let path = require('path');

const DIR = path.resolve(__dirname, 'public/jsx')

module.exports = {
  entry: path.join(DIR, "JournalApp.jsx"),
  output: {
    path: path.join(DIR, "target"),
    filename: "JournalBundle.jsx"
  },
  module: {

    rules: [
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
