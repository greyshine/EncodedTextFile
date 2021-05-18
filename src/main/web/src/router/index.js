import {createRouter, createWebHistory} from "vue-router";
import utils from "@/assets/utils"
import Home from "../views/Home.vue";
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

const routes2 = [
  {
    path: "/",
    name: "Home",
    component: Home,
  },
  {
    path: "/about",
    name: "About",
    // route level code-splitting
    // this generates a separate chunk (about.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () =>
        import(/* webpackChunkName: "about" */ "../views/About.vue"),
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {

  console.log('router:: router.beforeEach');
  console.log('router:: to', to);
  console.log('router:: from', from);
  console.log('router:: next', next);

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
