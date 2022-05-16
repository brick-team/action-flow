import {createRouter, createWebHashHistory} from 'vue-router'

const routes = [{
    path: '/action',
    name: 'action',
    component: () =>
        import ('../views/Action.vue')
},
    {
        path: '/flow',
        name: 'home',
        component: () =>
            import ('../views/Home.vue')
    }
    ]

const router = createRouter({
    history: createWebHashHistory(),
    routes
})

export default router;