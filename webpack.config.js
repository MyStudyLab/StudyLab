let path = require('path');
let uglify = require('uglifyjs-webpack-plugin');

const SRC_DIR = path.resolve(__dirname, 'public/src');
const OUT_DIR = path.resolve(__dirname, 'public/target');

module.exports = {
    entry: path.join(SRC_DIR, "jsx", "JournalApp.jsx"),
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
                include: path.join(SRC_DIR, "jsx"),
                use: {
                    loader: 'babel-loader'
                }
            },
            // CSS files are treated as modules
            {
                test: /\.css$/,
                include: path.join(SRC_DIR, "css"),
                use: [
                    {loader: 'style-loader'},
                    {loader: 'css-loader'}
                ]
            }
        ]

    }
};
