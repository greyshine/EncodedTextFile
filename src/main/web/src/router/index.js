import {createRouter, createWebHistory} from "vue-router";
import utils from "@/assets/utils"
import Textinput from "@/components/Textinput";
import Login from "@/components/Login";

const routes = [
  {
    path: "/",
    component: Textinput
  },
  {
    path: "/login",
    component: Login
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {

  //console.log('router:: router.beforeEach');
  //console.log('router:: to', to);
  //console.log('router:: from',from);
  //console.log('router:: next',next);

  const tokenExists = utils.getToken() != null;
  console.log('router:: token exists', tokenExists, utils.getToken());

  switch (to.fullPath) {

    case '/':

      if (!tokenExists) {
        console.log('router:: /->/login forward to login');
        next('/login');
        return;
      }

      break;

    case '/login':

      if (tokenExists) {
        console.log('router:: /login->/ forward to view');
        next('/');
        return;
      }

      break;
  }

  console.log('router:: default handling');
  next();
})

export default router;
