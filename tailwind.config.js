/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/main/resources/templates/**/*.html',
    './src/main/resources/static/js/**/*.js',
  ],
  darkMode: 'class',   // toggled by adding 'dark' to <html>
  // Preflight (Tailwind's CSS reset) is off: templates are currently styled with
  // inline styles + the head.html <style> block, and the old CDN script was
  // blocked by our CSP anyway — enabling the reset now would change rendering.
  // Re-enable when templates migrate to utility classes.
  corePlugins: { preflight: false },
  theme: {
    extend: {

      // ── Typography ─────────────────────────────────────────────────
      fontFamily: {
        serif: ['"Playfair Display"', 'Georgia', 'serif'],
        sans:  ['"Inter"', 'system-ui', 'sans-serif'],
      },

      fontSize: {
        // Editorial scale
        'display': ['2.5rem', { lineHeight: '1.15', letterSpacing: '-0.03em', fontWeight: '700' }],
        'hero':    ['2rem',   { lineHeight: '1.2',  letterSpacing: '-0.025em', fontWeight: '700' }],
        'h1':      ['1.75rem',{ lineHeight: '1.25', letterSpacing: '-0.02em', fontWeight: '700' }],
        'h2':      ['1.375rem',{lineHeight: '1.3',  letterSpacing: '-0.015em', fontWeight: '500' }],
        'h3':      ['1.125rem',{lineHeight: '1.4',  fontWeight: '500' }],
        'body-lg': ['1rem',   { lineHeight: '1.85' }],
        'body':    ['0.9375rem', { lineHeight: '1.75' }],
        'caption': ['0.8125rem', { lineHeight: '1.5' }],
        'label':   ['0.6875rem', { lineHeight: '1.4', letterSpacing: '0.08em' }],
      },

      // ── Category / topic color palette ─────────────────────────────
      // Each topic has a text color + background pill color
      colors: {
        topic: {
          technology: {
            text: '#185FA5',
            bg:   '#E6F1FB',
          },
          culture: {
            text: '#993556',
            bg:   '#FBEAF0',
          },
          science: {
            text: '#854F0B',
            bg:   '#FAEEDA',
          },
          opinion: {
            text: '#534AB7',
            bg:   '#EEEDFE',
          },
          environment: {
            text: '#0F6E56',
            bg:   '#E1F5EE',
          },
        },

        // Brand accent (newsletter green)
        brand: {
          DEFAULT: '#1D9E75',
          dark:    '#0F6E56',
          light:   '#E1F5EE',
        },

        // Neutral surface scale (dark theme base)
        surface: {
          DEFAULT: '#0F0F0F',
          raised:  '#181818',
          overlay: '#222222',
          border:  '#2E2E2E',
          muted:   '#3A3A3A',
        },

        // Text scale
        ink: {
          primary:   '#F2F2F0',
          secondary: '#A8A8A0',
          tertiary:  '#6A6A62',
        },
      },

      // ── Spacing ────────────────────────────────────────────────────
      maxWidth: {
        'prose-sm':  '540px',
        'prose':     '680px',
        'prose-lg':  '760px',
        'layout':    '1200px',
      },

      // ── Border radius ──────────────────────────────────────────────
      borderRadius: {
        'tag':  '4px',
        'card': '12px',
        'xl':   '16px',
      },

      // ── Box shadows ────────────────────────────────────────────────
      boxShadow: {
        'card': '0 1px 3px rgba(0,0,0,0.4), 0 1px 2px rgba(0,0,0,0.3)',
      },

      // ── Animation ──────────────────────────────────────────────────
      transitionTimingFunction: {
        'editorial': 'cubic-bezier(0.25, 0.1, 0.25, 1)',
      },
    },
  },

  plugins: [
    require('@tailwindcss/typography'),
    require('@tailwindcss/forms'),
  ],
};
