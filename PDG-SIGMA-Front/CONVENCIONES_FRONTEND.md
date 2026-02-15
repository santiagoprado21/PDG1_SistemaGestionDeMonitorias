# 🧭 CONVENCIONES FRONTEND — PDG-SIGMA-Front

Guía oficial de identidad visual y desarrollo de interfaces para el proyecto **PDG-SIGMA-Front**. Este documento asegura la consistencia entre vistas, componentes y estilos, respetando la identidad institucional definida por el manual de marca (marzo 2025).

---

## 🧩 1. Principios Base

- **Coherencia visual:** Todos los componentes y pantallas deben usar los tokens y estructuras aquí definidas.
- **CSS desacoplado:** Cada componente React utiliza su propio archivo `.css` (`Componente.css`).
- **Escalabilidad:** Las clases CSS deben ser reutilizables y predecibles.
- **Accesibilidad:** Cumplir con contrastes y jerarquía visuales suficientes (WCAG 2.1 AA).

---

## 🎨 2. Design Tokens Globales
Agrega las siguientes variables al archivo `src/index.css` (antes de cualquier otro estilo global):

```css
:root {
  /* 🎨 Colores institucionales */
  --color-primary: #5555EA;           /* Azul institucional */
  --color-primary-hover: #4444D8;     /* Hover */
  --color-secondary: #4B5563;         /* Gris principal */
  --color-secondary-bg: #F9FAFB;      /* Fondos suaves */
  --color-danger: #D32F2F;            /* Rojo de alerta */
  --color-success: #10B981;           /* Verde de confirmación */
  --color-border: #E5E7EB;            /* Bordes suaves */
  --color-text-primary: #111827;      /* Texto principal */
  --color-text-secondary: #4B5563;    /* Texto auxiliar */
  --color-bg-body: #FFFFFF;           /* Fondo general */
  --color-bg-alt: #F3F4F6;            /* Fondo alternativo */
  --color-focus: #93C5FD;             /* Azul accesible foco */

  /* ✏️ Tipografía */
  --font-primary: 'Plus Jakarta Sans', sans-serif;
  --font-secondary: 'Segoe UI', sans-serif;

  /* 🔠 Tamaños tipográficos base */
  --fs-h1: 28.4px;  /* bold */
  --fs-h2: 18.2px;  /* semibold */
  --fs-h3: 16.7px;  /* semibold */
  --fs-h4: 13.2px;  /* semibold */
  --fs-body: 12.8px; /* regular */

  /* 🔳 Espaciado (8pt Grid) */
  --space-0: 0px;
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-8: 32px;
  --space-10: 40px;

  /* 🔲 Bordes y radios */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;

  /* 🌫️ Sombras */
  --shadow-sm: 0 1px 2px rgba(0,0,0,0.05);
  --shadow-md: 0 4px 8px rgba(0,0,0,0.10);
  --shadow-lg: 0 10px 20px rgba(0,0,0,0.15);

  /* 💫 Transiciones */
  --tr-fast: 0.15s ease-in-out;
}

body {
  font-family: var(--font-primary);
  background: var(--color-bg-body);
  color: var(--color-text-primary);
}
```

---

## 🧱 3. Estructura CSS por componente
Cada archivo `.css` debe seguir esta estructura:

```css
/* COMPONENTE: NombreDelComponente */

/* 1️⃣ Layout */
.container { ... }

/* 2️⃣ Elementos principales */
.header {...}
.title {...}

/* 3️⃣ Estados */
.button:hover {...}
.button:disabled {...}

/* 4️⃣ Responsividad */
@media (max-width: 768px) {
  ...
}
```

📘 Recomendación: usa nombres predecibles con prefijos cuando sea necesario.
- Evita `.red`, `.box`, `.main` genéricos. Usa `.navbar-container`, `.login-form`.

---

## 🧭 4. Tipografía y jerarquía visual

### Encabezados globales
```css
h1 { font-size: var(--fs-h1); font-weight: 700; }
h2 { font-size: var(--fs-h2); font-weight: 600; }
h3 { font-size: var(--fs-h3); font-weight: 600; }
h4 { font-size: var(--fs-h4); font-weight: 600; }

p, span, li { font-size: var(--fs-body); }
.small { font-size: 11px; color: var(--color-text-secondary); }
```

**Uso recomendado:**
- `h1`: título de vista o módulo.
- `h2`: secciones principales.
- `h3`: subtítulos o agrupaciones menores.
- `h4`: etiquetas de campo o microencabezados.

**Cuerpo de texto:**
- Fuente: Plus Jakarta Sans Regular 400
- Color: `--color-text-primary`
- Line-height: 1.5–1.6

---

## 🔘 5. Botones (Button.css)

```css
.btn {
  font-family: var(--font-primary);
  font-size: 13px;
  font-weight: 600;
  border-radius: var(--radius-md);
  padding: 6px 32px;
  cursor: pointer;
  border: none;
  transition: all var(--tr-fast);
}

.btn-primary {
  background: var(--color-primary);
  color: #fff;
  box-shadow: var(--shadow-sm);
}
.btn-primary:hover {
  background: var(--color-primary-hover);
  box-shadow: var(--shadow-md);
}

.btn-secondary {
  background: transparent;
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}
.btn-secondary:hover {
  background: var(--color-secondary-bg);
}

.btn-danger {
  background: var(--color-danger);
  color: #fff;
}
.btn-danger:hover {
  background: #b71c1c;
}
```

**Buenas prácticas:**
- Un `btn-primary` por pantalla.
- `btn-secondary` para cancelar o navegar.
- `btn-danger` para eliminar o confirmar destrucción.

---

## 🪞 6. Formularios e Inputs

```css
.input {
  width: 100%;
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-family: var(--font-primary);
  transition: border-color var(--tr-fast);
}
.input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px #5555ea33;
}
.input[aria-invalid="true"] {
  border-color: var(--color-danger);
}
```

Complementa con `.label`, `.form-group`, `.error-message`, etc.

---

## 🧭 7. Componentes comunes y reutilizables

### VerticalNavbar
```css
.navbar {
  background: var(--color-primary);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 220px;
  min-height: 100vh;
}
.nav-item { padding: 12px; text-align: center; cursor: pointer; }
.nav-item:hover { background: var(--color-primary-hover); }
.nav-item.active { background: #4444d8; font-weight: 600; }
```

### PopUp / Modal
```css
.popup-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center;
}
.popup-content {
  background: #fff;
  border-radius: var(--radius-md);
  padding: var(--space-6);
  box-shadow: var(--shadow-lg);
  max-width: 500px;
}
```

### LoadingSpinner
```css
.spinner {
  width: 40px; height: 40px;
  border-radius: 50%;
  border: 4px solid #E5E7EB;
  border-top-color: var(--color-primary);
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
```

---

## 🚀 8. Jerarquía de Carpetas recomendada para componentes UI

```
src/ui/
├── buttons/
│   ├── ButtonPrimary.js
│   ├── ButtonSecondary.js
│   ├── ButtonDanger.js
│   └── Button.css
├── typography/
│   ├── H1.js
│   ├── H2.js
│   ├── Body.js
│   └── Typography.css
├── layout/
│   ├── NavbarVertical.js
│   ├── LayoutWrapper.js
│   └── Layout.css
└── forms/
    ├── Input.js
    ├── Label.js
    └── FormGroup.css
```

📘 Esto no requiere refactor inmediato, pero sí un estándar para nuevos módulos.

---

## ♿ 9. Accesibilidad básica (A11y)
- Contraste mínimo 4.5:1.
- Focus siempre visible (`outline` o sombra azul suave).
- Etiquetas `aria-label`, `aria-invalid`, `role` y `aria-live` en elementos dinámicos.

---

## 🎭 10. Efectos, transiciones y movimiento
```css
.fade-in { opacity: 0; animation: fadeIn 0.3s forwards; }
@keyframes fadeIn { to { opacity: 1; } }

.slide-up { transform: translateY(10px); animation: slideUp 0.3s forwards; }
@keyframes slideUp { to { transform: translateY(0); } }
```
Usar movimiento solo para feedback visual (transiciones suaves, microanimaciones).

---

## 🧾 11. Checklist visual de consistencia

- [ ] Colores solo de la paleta institucional (`--color-*`)
- [ ] Tipografía global: Plus Jakarta Sans
- [ ] Reutiliza tokens CSS para margen y tipografía
- [ ] Botones y estados sólidos, sin variaciones de tono no definidas
- [ ] Focus visible siempre
- [ ] No sombras excesivas ni fondos arbitrarios
- [ ] Compatible con modo de alto contraste (tema futuro)

---

## 🧱 12. Recomendaciones para desarrollo

- Revisa `index.css` antes de crear nuevos estilos globales.
- Prefiere clases sobre estilos inline (excepto props dinámicos en React).
- Usa variables `var(--token)` siempre que sea posible.
- Comenta secciones CSS con `/* === Bloque === */`.
- Usa ESLint y Prettier para mantener consistencia de formato.

---

## 📅 Última actualización: Noviembre 2025
Autor: Equipo PDG-SIGMA Frontend — Basado en Identidad Institucional 2025
