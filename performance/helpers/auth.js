import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../config/env.js';

/**
 * Realiza el login contra SIGMA+ y devuelve el token JWT.
 * Retorna null si la autenticación falla.
 *
 * @param {string} userId   - ID del usuario (código de estudiante, profesor o jefe)
 * @param {string} password - Contraseña del usuario
 * @returns {string|null}   - Token JWT o null si falla
 */
export function login(userId, password) {
    const payload = JSON.stringify({ userId, password });
    const params  = { headers: { 'Content-Type': 'application/json' } };

    const res = http.post(`${BASE_URL}/auth/login`, payload, params);

    const ok = check(res, {
        '[auth] login status 200': (r) => r.status === 200,
        '[auth] responde con token': (r) => {
            try { return !!r.json('token'); } catch(_) { return false; }
        }
    });

    if (!ok || res.status !== 200) {
        console.warn(`[auth] login fallido para ${userId} — status: ${res.status} — body: ${res.body}`);
        return null;
    }

    return res.json('token');
}

/**
 * Construye los headers de autorización para las peticiones autenticadas.
 *
 * @param {string} token - Token JWT obtenido con login()
 * @returns {object}     - Headers listos para pasar a http.get/post
 */
export function authHeaders(token) {
    return {
        headers: {
            'Content-Type':  'application/json',
            'Authorization': `Bearer ${token}`
        }
    };
}
