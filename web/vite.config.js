import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({
    // github pages serves a project site from a subpath, so every asset url and
    // every internal link has to be relative to /Snuff-AC/
    base: '/Snuff-AC/',
    plugins: [react()],
    build: {
        rollupOptions: {
            input: {
                main: 'index.html',
                changelog: 'changelog.html',
            },
        },
    },
});
