# Convenciones Frontend — PDG-SIGMA-Front (Version Integrada)

Guia oficial de identidad visual y desarrollo de interfaces para el proyecto PDG-SIGMA-Front. Este documento asegura la consistencia entre vistas, componentes y estilos, respetando la identidad institucional definida por el manual de marca (marzo 2025).

## 1. Principios base
- Coherencia visual: Todos los componentes y pantallas deben usar los tokens y estructuras aqui definidas.
- CSS desacoplado: Cada componente React utiliza su propio archivo .css (Componente.css).
- Escalabilidad: Las clases CSS deben ser reutilizables y predecibles.
- Accesibilidad: Cumplir con contrastes y jerarquia visuales suficientes (WCAG 2.1 AA).

## 2. Design tokens globales

Agrega las siguientes variables al archivo src/index.css. Se han incluido los colores exactos del manual de marca.

:root {
  /* Colores institucionales (Manual de Marca 2025) */
  --color-primary: #5454e9;           /* Azul Icesi oficial */
  --color-primary-hover: #4444D8;     
  
  /* Paleta complementaria institucional */
  --color-icesi-yellow: #e4eb60;
  --color-icesi-orange: #e9683b;
  --color-icesi-green: #4cb979;
  --color-icesi-purple: #865cf0;

  /* Grises y Neutros */
  --color-secondary: #4B5563;         
  --color-secondary-bg: #F9FAFB;      
  --color-icesi-gray-1: #88898c;      /* Gris institucional 1 */
  --color-icesi-gray-2: #cecfd4;      /* Gris institucional 2 */
  --color-border: #E5E7EB;            
  --color-text-primary: #111827;      
  --color-text-secondary: #4B5563;    
  --color-bg-body: #FFFFFF;           
  --color-bg-alt: #F3F4F6;            

  /* Estados */
  --color-danger: #D32F2F;            
  --color-success: #10B981;           
  --color-focus: #93C5FD;             

  /* Tipografia */
  --font-primary: 'Plus Jakarta Sans', sans-serif;
  --font-secondary: 'Segoe UI', sans-serif; /* Solo como fallback */

  /* Tamaños tipograficos base */
  --fs-h1: 28.4px;  
  --fs-h2: 18.2px;  
  --fs-h3: 16.7px;  
  --fs-h4: 13.2px;  
  --fs-body: 12.8px; 

  /* Espaciado (8pt Grid) */
  --space-1: 4px; --space-2: 8px; --space-3: 12px; --space-4: 16px;
  --space-6: 24px; --space-8: 32px; --space-10: 40px;

  /* Bordes y radios */
  --radius-sm: 4px; --radius-md: 8px; --radius-lg: 12px;

  /* Sombras */
  --shadow-sm: 0 1px 2px rgba(0,0,0,0.05);
  --shadow-md: 0 4px 8px rgba(0,0,0,0.10);
  --shadow-lg: 0 10px 20px rgba(0,0,0,0.15);

  /* Transiciones */
  --tr-fast: 0.15s ease-in-out;
}


Regla de oro de color: La paleta complementaria (yellow, orange, green, purple) nunca se usa sola; siempre debe estar acompanada por el Azul Icesi (--color-primary) y blanco.

## 3. Tipografia y reglas de uso

### 3.1 Jerarquia visual
h1 { font-size: var(--fs-h1); font-weight: 700; }
h2 { font-size: var(--fs-h2); font-weight: 600; }
h3 { font-size: var(--fs-h3); font-weight: 600; }
h4 { font-size: var(--fs-h4); font-weight: 600; }

p, span, li { font-size: var(--fs-body); line-height: 1.5; }

### 3.2 Restricciones del manual
- Fuente unica: Plus Jakarta Sans es la unica permitida en la UI. Arial solo se usa para exportaciones a Office.
- Alineacion: Por defecto siempre a la izquierda. Centrado solo para numeros destacados o banners cortos.
- Mayusculas: No usar mayusculas sostenidas para parrafos. Solo para siglas (SIGMA, PDG) o etiquetas de botones muy cortos.
- Estilo: No usar cursivas/italicas, salvo para terminos tecnicos en otros idiomas o citas.

## 4. Estructura CSS por componente

Cada archivo .css debe seguir este orden:

/* COMPONENTE: NombreDelComponente */
/* 1. Layout (.container) */
/* 2. Elementos principales (.header, .title) */
/* 3. Estados (:hover, :disabled) */
/* 4. Responsividad (@media) */

## 5. Componentes de interfaz (UI)

### 5.1 Botones (Button.css)
- Un btn-primary por pantalla.
- btn-secondary para cancelar o navegar.

.btn {
  font-family: var(--font-primary);
  font-size: 13px; font-weight: 600;
  border-radius: var(--radius-md);
  padding: 6px 32px; cursor: pointer;
  border: none; transition: all var(--tr-fast);
}
.btn-primary { background: var(--color-primary); color: #fff; }
.btn-danger { background: var(--color-danger); color: #fff; }

### 5.2 Formularios e inputs
.input {
  width: 100%; padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}
.input:focus {
  outline: none; border-color: var(--color-primary);
  box-shadow: 0 0 0 2px #5454e933;
}

### 5.3 Iconografia
- Estilo outline (trazo), sin rellenos planos.
- Esquinas angulares, sin redondeos.

.icon {
  stroke: var(--color-primary); fill: transparent;
  stroke-width: 1.5px; stroke-linecap: butt; stroke-linejoin: miter;
}

## 6. Identidad institucional en la app

### 6.1 Uso del logo
- Ubicacion: Zona superior izquierda (Navbar).
- Tamano minimo: 32px de alto en movil, 44px en desktop.
- Prohibido: No aplicar sombras, no deformar, no cambiar colores con filtros.

### 6.2 Cenefas y descriptores

Usa estos recursos para enmarcar secciones o indicar pertenencia academica.

.cinta-icesi {
  background: var(--color-primary); color: #fff;
  padding: var(--space-2) var(--space-4); border-radius: var(--radius-sm);
}
.descriptor-facultad {
  background: var(--color-icesi-green); color: #fff;
  font-size: 12px; padding: 2px 12px; border-radius: 999px;
}

### 6.3 Tagline "Llega mas lejos"
- Solo en pantallas de bienvenida (Splash) o banners informativos.
- Nunca usar como texto dentro de un boton o pegado al logo.

## 7. Layout y responsividad

### 7.1 Breakpoints oficiales
- Movil: max-width: 768px (Formularios en 1 columna, Navbar colapsado).
- Tablet: 769px - 1024px (Cards en 2 columnas).
- Desktop: min-width: 1025px (Navbar fijo 220px, Cards en 3-4 columnas).

### 7.2 Estilo fotografico
- Usar fotos reales de la universidad.
- Aplicar filtro suave para consistencia: filter: saturate(0.9) contrast(1.02);.

## 8. Accesibilidad y movimiento
- Contraste: Minimo 4.5:1.
- Focus: Siempre visible con sombra azul suave.
- Animaciones: Solo para feedback visual (fade-in, slide-up de 0.3s).

## 9. Jerarquia de carpetas UI
src/ui/
├── buttons/      # ButtonPrimary.js, Button.css
├── typography/   # H1.js, Body.js, Typography.css
├── layout/       # NavbarVertical.js, Layout.css
└── forms/        # Input.js, FormGroup.css

## 10. Checklist de consistencia
- Colores solo de la paleta institucional (--color-*).
- Tipografia Plus Jakarta Sans sin italicas ni mayusculas sostenidas.
- Logo en tamano correcto y sin efectos prohibidos.
- Iconos solo con trazo y esquinas angulares.
- Textos alineados a la izquierda.

Ultima actualizacion: 2 de Marzo 2026

Autor: Equipo PDG-SIGMA Frontend — Basado en Manual de Identidad Marzo 2025