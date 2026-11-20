# Fotos de las piezas de AI Tuning

Pon aquí una foto por pieza, con el **mismo id** que tiene en
`app/src/main/assets/parts_catalog.json`:

```
rin_18_5radios_negro.jpg
aero_ala_gt.png
escape_titanio_valvulado.webp
```

Formatos: `.png`, `.jpg`, `.jpeg` o `.webp`. Funcionan mejor con fondo blanco
o liso, una sola pieza y buena resolución (la app las reduce a 1536 px).

Luego corre, desde la carpeta `seed/`:

```bash
node seed.js --parts
```

El script sube cada foto a Storage (`parts/<id>.<ext>`) y le pone su
`imageUrl` a la pieza en Firestore. Las piezas sin foto siguen funcionando:
la app muestra un ícono y la AI las dibuja solo a partir de su descripción.

Guarda en `sourceUrl` (en el JSON) la página de donde salió cada foto y su
precio, para dar crédito y poder verificarlo.
