<template>

  <div id="heading">
    <div>EncodedTextFile</div>
    <div :style="{display:displayButtons}" class="buttons">
      <button @click="save">Save</button>
      <button @click="logout">Logout</button>
    </div>
  </div>

  <div id="message" :style="{'display':message==null?'none':'block'}">{{ message }}</div>

  <!--router-link to="/login">Login</router-link-->
  <!--router-link to="/" ></router-link-->
  <router-view/>

</template>

<script>
import {defineComponent} from "vue";
import mitt from 'mitt'
import utils from "@/assets/utils";

export default defineComponent({

  name: 'App',

  data() {
    return {
      displayButtons: 'none',
      message: null
    }
  },

  created() {

    this.$root.bus = mitt();

    this.$root.bus.on('showButtons', (value) => {

      if (value === 0) {
        this.displayButtons = 'none';
      } else if (value === 1) {
        this.displayButtons = 'inline';
      }
    });

    this.$root.bus.on('message', value => this.setMessage(value));
  },

  mounted() {
    console.log('mounted');
  },

  methods: {

    setMessage(message) {
      this.message = message == null ? null : new Date().toISOString() + ': ' + message;
    },

    logout() {

      console.log('logout...');
      this.$root.bus.emit('message', null);

      const tokenValue = utils.getToken();

      if (tokenValue == null) {
        this.$router.replace('/login');
        return;
      }

      const options = {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8',
          'token': tokenValue
        }
      };

      fetch('/api/logout', options)
          .then(response => {
            console.log("logout", response.status, response);
          })
          .catch(exception => this.$root.bus.emit('message', exception))
          .finally(() => {

            console.log('setting null...');
            utils.setToken(null);
            this.displayButtons = 'none';

            this.$router.replace('/login');
          });
    },

    save() {
      this.$root.bus.emit('save');
    }
  }
});
</script>

<style lang="scss">
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;

  div#heading {
    text-align: left;
    padding: 5px;
    background-color: beige;
    color: lightgray;
    font-size: 1em;
    font-style: italic;

    button {
      border: 0px;
    }
  }

  div#message {
    text-align: left;
    padding: 5px;
    margin: 5px;
    color: tomato;
    border-left: 1px solid tomato;
    background-color: cornsilk;
  }
}

#nav {
  padding: 30px;

  a {
    font-weight: bold;
    color: #2c3e50;

    &.router-link-exact-active {
      color: #42b983;
    }
  }
}
</style>
