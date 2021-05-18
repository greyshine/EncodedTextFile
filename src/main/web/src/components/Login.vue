<template>

  <div>
    <form :style="{display:display}" @submit.prevent="submit">
      <input v-model="password" type="password"/>
      <button size="sm" type="submit">enter</button>
    </form>
  </div>

</template>

<script>
import utils from '@/assets/utils'

export default {

  name: 'Login',

  data() {
    return {
      password: null,
      display: 'block'
    }
  },

  methods: {

    submit($event) {

      if (this.password == null || this.password.trim() == '') {
        this.password = '';
        return;
      }

      utils.setToken(null);

      const options = {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8',
          'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify({
          password: this.password,
        })
      };

      fetch('/api/login', options)
          .then(async response => {

            if (response.status != 200) {
              console.error("Login failure", response);
              this.$root.bus.emit('message', 'Login Failure: ' + response.status + " - " + response.statusText);
              return;
            }

            utils.setToken(await response.text());
            this.$root.bus.emit('message');
            this.$root.bus.emit('showButtons', 1);
            this.$router.push({path: '/'});
          })
          .catch(e => {
            console.error(e)
            this.$root.bus.emit('message', 'Login Failure (exception=' + e + ')');

          });
    }
  }
}
</script>

<style lang="scss" scoped>

div {
  text-align: left;

  input {
    text-align: left;
    margin-top: 3em;
    width: 100px;
    outline: none;
    background-color: gainsboro;
    border: none;
    border-radius: 5px;
  }

  input:focus {
    background-color: aliceblue;
    outline: none;
  }


}


button {
  margin-top: 10px
}

.error {
  border-color: tomato;
}
</style>