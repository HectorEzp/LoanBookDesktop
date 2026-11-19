# Biblioteca · Aplicación de escritorio (Java Swing + JDBC + JSch)

## Requisitos
- JDK 17 o superior y Maven
- La instancia EC2 en ejecución (puerto 22 abierto para tu IP pública en `sg-web`)
- Archivo `.pem` de la EC2 guardado fuera del repositorio

## Configuración
```bash
cp config.properties.example config.properties
# editar config.properties con tus datos (IP de la EC2, ruta del .pem, endpoint de RDS, password de app_desktop)
```

## Compilar y ejecutar
```bash
mvn clean package
java -jar target/biblioteca-desktop.jar
```
Ejecuta el comando desde esta carpeta (donde está `config.properties`).

## Qué hace
1. Abre el túnel SSH hacia la EC2 (JSch) y reenvía `127.0.0.1:3307` al endpoint privado de RDS.
2. Se conecta por JDBC con el usuario `app_desktop`.
3. CRUD de **préstamos** y **libros**, con el botón *Actualizar / Refrescar tabla*.
4. Al cerrar la ventana, cierra la conexión JDBC y la sesión SSH.
