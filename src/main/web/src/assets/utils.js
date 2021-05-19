let tokenValue = null;

export default {

    hw() {
        console.log('helloworld');
    },

    getToken() {
      return tokenValue;
    },

    setToken(token) {
        tokenValue = typeof token != 'string' || token.trim() == '' ? null : token;
    },

    isToken(token) {
        return token != null && tokenValue === token;
    },

    addTokenToHeader(header) {
        if (typeof header != 'object') {
            return header;
        }
        header['token'] = tokenValue;
        return header;
    }

};