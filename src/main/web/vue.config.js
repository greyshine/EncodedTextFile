module.exports = {

    /**
     * Disable eslint
     *
     * https://techinplanet.com/disable-eslint-vue-js/
     *
     * Also see: package.json//eslintConfig
     *
     */
    lintOnSave: false,

    devServer: {
        port: 8081,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                ws: true,
                changeOrigin: true
            }
        }
    }
};