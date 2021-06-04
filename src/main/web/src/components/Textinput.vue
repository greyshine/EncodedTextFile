<template>

  // TODO: show textarea only after successfull load/save
  // TODO: hide when saving!

  <img src="@/assets/waiting.gif"/>

  <textarea id="text" v-model="text" :disabled="disabled" class="m1-lr" placeholder="text..."
            @focus="focusTextarea"></textarea>

</template>

<script>
import utils from '@/assets/utils';

export default {

  name: 'Textinput',

  data() {
    return {
      text: '',
      disabled: false
    };
  },

  beforeCreate() {
    console.log('beforeCreated()');
    this.$root.bus.on('load', () => this.load());
    this.$root.bus.on('save', () => this.save());
    console.log('beforeCreated().');
  },

  created() {
  },

  mounted() {
    this.load();
  },

  methods: {

    save() {

      this.$root.bus.emit('message');
      this.disabled = true;

      const tokenValue = utils.getToken();

      const options = {
        method: 'POST',
        headers: {
          'Content-Type': 'text/plain',
          'token': tokenValue
        },
        body: this.text
      };

      fetch('/api/data', options)
          .then(response => {
            if (response.status != 200) {
              console.error("fail", response);
              this.$root.bus.emit('message', response.status + ' - ' + response.statusText);
              return;
            }

            this.$root.bus.emit('message', 'saved');
          })
          .finally(() => this.disabled = false);
    },

    load() {

      console.log('load', utils.getToken(), typeof utils.getToken());
      this.$root.bus.emit('message');

      this.disabled = true;

      const options = {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8',
          'token': utils.getToken()
        }
      };

      fetch('/api/data', options)
          .then(async response => {

            console.log('load', response);

            const textdata = await response.text();

            if (response.status != 200) {
              this.disableUpdate = true;
              console.error(textdata);
              this.$root.bus.emit('message', textdata);
              return;
            }

            this.text = textdata;
          })
          .finally(() => {
            this.disabled = false;
          });
    },

    focusTextarea() {
      this.$root.bus.emit('message');
    }
  }

}
</script>


<style lang="scss" scoped>
textarea {
  /* Height "auto" will allow the text area to expand vertically in size with a horizontal scrollbar if pre-existing content is added to the box before rendering. Remove this if you want a pre-set height. Use "em" to match the font size set in the website. */
  height: auto;
  /* Use "em" to define the height based on the text size set in your website and the text rows in the box, not a static pixel value. */
  min-height: 30em;
  width: 98%;
  resize: vertical;
}
</style>