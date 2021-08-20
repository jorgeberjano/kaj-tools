![Logo](./src/main/resources/images/icon.png?at=refs%2Fheads%2Fdevelop)

# KAJ-TOOLS

Kafka-Avro-Json Tools



## Introducción

La biblioteca de clases *KAJ Tools* proporciona un marco de trabajo para crear aplicaciones específicas que sirvan como herramienta para facilitar las pruebas en entornos que usen *Kafka*, *Schema Registry* y serialización *AVRO*.

Permite realizar, fundamentalmente, las siguientes tareas:

- Producir mensajes de prueba por un *topic* partiendo simplemente de contenidos JSON.
- Producir mensajes de prueba a partir de *JSON* que se cargan en las clases *AVRO* definidas en nuestras bibliotecas de clases.
- Producir mensajes de prueba a partir de plantillas.
- Consumir  mensajes que se han enviado a un *topic* y visualizarlos en formato JSON.
- Consumir  mensajes que se han enviado a un *topic*, cargarlos en las clases *AVRO* definidas en nuestras bibliotecas de clases y visualizarlos en formato JSON.
- Comparar los esquemas de las clases *AVRO* con las almacenadas en el *Schema Regristry* y mostrar sus diferencias.
- Consultar esquemas del *Schema Regristry* y mostrar las diferencias con esquemas anteriores.
- Borrar esquemas del *Schema Regristry*.

- Generar *JSO*N de ejemplo a partir de clases *AVRO* o de cualquier clase *Java Bean* en general.
- Generar plantillas a partir de contenido JSON.



## Entornos

Los entornos se definen en el fichero de recursos `environments.yml` dentro de la aplicación específica.

Para cada entorno se definen las siguientes propiedades:

```
- name: nombre del entorno
  bootstrapServers: kafkaserver1:9092, kafkaserver2:9092
  urlSchemaRegistry: http://schemaregistryserver/
  userSchemaRegistry: userName1
  passwordSchemaRegistry: password1
  autoRegisterSchemas: true
  securityProtocol: SASL_SSL
  saslMechanism: SCRAM-SHA-512
  sslTruststoreLocation: jks/truststore.jks
  sslTruststorePassword: truststore_password
  saslJaasConfig: org.apache.kafka.common.security.scram.ScramLoginModule required username="userName2" password="password2";
```



## Producción de mensaje Kafka

![Kafka](./src/main/resources/images/kafka.png?at=refs%2Fheads%2Fdevelop)

La inyección de menasajes al ecosistema Kafka se realiza desde el panel que muestra la pestaña lateral Producer.

Para simplificar se usa el término *VALUE* para referirse al valor del mensaje y el término *KEY* para referirse a la clave del mensaje.

Se debe seleccionar el entorno en el que vamos a operar antes de enviar un mensaje.

Para simplificar la localización e un productor determinado se puede seleccionar previamente un dominio, de tal manera que las opciones para elegir un productor se reducen a los productores de mensajes de dicho dominio. Si no se selecciona ningún dominio se puede seleccionar el productor genérico.



### GenericProducer

Normalmente las clases de los objetos productores deben ser declarados dentro del código de la aplicación para que estén disponibles, sin embargo hay un objeto productor genérico que permite inyectar mensajes creados a partir de contenido *JSON* sin conocimiento de las clases AVRO, completamente agnóstico a nuestras librerías. Este productor se llama `GenericProducer`.

Para enviar mensajes, en primer lugar, hay que seleccionar el entorno al que van dirigidos los mensajes. 

Luego hay que seleccionar el productor `GenericProducer`.

A continuación, seleccionar o introducir directamente el nombre del *topic* por el que se desean inyectar los mensajes. Con el productor genérico en el combo se muestran todos los *topics* declarados por los productores específicos. Pero no hay que limitarse a ese conjunto, por el contrario, se puede usar cualquiera escribiendo su nombre directamente en el campo de edición.

También hay que indicar el KEY y VALUE a enviar. Esto se puede hacer de varias formas:

- Seleccionando con los *combo boxes* una archivo de recurso.
- Pulsando el botón ![folder](./src/main/resources/images/folder.png?at=refs%2Fheads%2Fdevelop)y seleccionar un archivo del sistema de archivos locales.
- Introducirlos directamente en el editor correspondiente.

De cualquier forma siempre se puede modificar el contenido de estos antes de ser enviados ya que lo que se envía es lo que hay en los editores de Key y Value.

El contenido de se puede especificar en formato *JSON* o en forma de plantilla, que se describirán mas adelante.

También es posible seleccionar la cantidad de mensajes a enviar.

Por último para realizar el envío del mensaje por el *topic* del entorno que se haya seleccionado hay que pulsar el botón ![comparar esquemas](./src/main/resources/images/enviar.png?at=refs%2Fheads%2Fdevelop)`Enviar`.



### Productores concretos

Los productores concreto se deben implementar en el código creando una clase que extendienda de la clase genérica AbstractClient e indicando las clase AVRO correspondientes al KEY y al VALUE

Por supuesto, antes hay que agregar al archivo `.pom` del proyecto, la dependencia Maven de la biblioteca de clases (*library*) donde se encuentran definidas las clases AVRO.

 Luego hay que crear una clase como la que sigue.

```java
@Component
public class MyClient extends AbstractClient<MyKey, MyValue> {
  @Getter
  private final String defaultTopic = "TOPIC-NAME";

  @Getter
  private final String folder = "my-domain/mytopic";

  public MyProducer() {
    super(MyKey.class, MyValue.class);
  }
}
```

Como se aprecia, en la clase hay que especificar:

- El *topic* por defecto (`defaultTopic`)
- La carpeta de recursos donde se almacenan los archivos `.json ` y `.template` que queremos que estén accesibles desde el combo box
- Las clases *Key* y *Value* en el constructor.

Opcionalmente se puede declarar una lista de topic si los mismos mensajes pueden ser inyectados en mas de un topic.

```java
@Getter
public List<String> availableTopics = Lists.newArrayList(defaultTopic,
      "ANOTHER-TOPIC-NAME");
```



También hay que crear los archivos `.json` o `.template` en la carpeta de recursos. Se recomienda organizarlos con, al menos, una carpeta por dominio.

Una vez compilada la aplicación  al ser ejecutada muestra en el combo box el nombre de la clase productora. Es importante no repetir los nombres de las clases para poder distinguirlas. Se recomienda prefijar el nombre del dominio en el nombre de la clase, aparte de crearla en el paquete de su dominio.

Cuando se selecciona una clase productora en el *combo box* en los respectivos del *KEY* y del *VALUE* estarán accesibles los archivos `.json `y `.template` que se hayan declarado en dicha clase.

Uno de los beneficios de crear productores concretos es que permiten que se pueda comparar los esquemas AVRO de las clases *Key* y *Value* que utilizan con las que están registradas en el *Schema Regristry*.



### Comparación de esquemas

Si se selecciona alguno de los productores específicos se puede usar el botón ![comparar esquemas](./src/main/resources/images/compare.png?at=refs%2Fheads%2Fdevelop) `Comparar esquemas` para realizar una comparación entre el esquema que declara la clase AVRO y el esquema que tiene registrado en *Schema Regristry* en el entorno concreto que se tenga seleccionado.

Si los esquemas no coinciden se mostrará en la consola de información las diferencias detectadas sobre los dos esquemas mostrando el texto no coincidente en un color con tono rojo.



### Plantillas

Para facilitar la inyección de datos aleatorios o preprogramados se ha usado la biblioteca **JsonTemplate** alojada en *GitHub* y con artefactos subidos a *Maven Central*. La *URL* de la biblioteca  es https://github.com/json-template/JsonTemplate.

Las plantillas proporcionan un leguaje específico de dominio para crear plantillas a partir de las cuales se genera contenido *JSON*.

Para la producción mensajes la aplicación permite especificar los contenidos *JSON* del *Key* y el *Value* tanto directamente en formato *JSON* o con la sintaxis que proporciona *JsonTemplate*.

La sintaxis es parecida a la de un *JSON* normal pero:

- Ni los atributos ni los literales de cadena se expresan entre comillas.
- Los valores se pueden expresar con productores de valores (*value producers*) que empiezan con @, por ejemplo @s genera una cadena aleatoria.
- Se pueden usar nombres de algunas variables que llevan el prefijo $.

Los principales productores de valores que proporciona la biblioteca es la siguiente:

| productor de valor | descripción                                           |
| ------------------ | ----------------------------------------------------- |
| @smart             | usado para conversión inteligente, se usa por defecto |
| @s                 | produce una cadena                                    |
| @i                 | produce un entero                                     |
| @f                 | produce un número en coma flotante                    |
| @b                 | produce un booleano                                   |
| @raw               | produce un contenido de cadena en bruto               |

Los productores personalizados son los siguientes:

| productor de valor | descripción                                                  |
| ------------------ | ------------------------------------------------------------ |
| @date              | produce la fecha actual en un formato determinado o uno por defecto. Para especificar un formato determinado se usa el mismo patrón que usa la clase de Java SimpleDateFormat. |
| @uuid              | produce un UUID o [Identificador único universal](https://es.wikipedia.org/wiki/Identificador_único_universal#:~:text=UUID se utilizó originalmente en,Open Software Foundation (OSF) .). Si se le suministra un parámetro indica la clave para compartir el valor entre diferentes atributos del JSON o entre atributos de los JSON de la Key y el Value. |
| @file              | produce una cadena elegida de forma aleatoria de entre todas las líneas de un fichero de texto. |
| @gs                | produce cadenas con valores aleatorios globales que pueden compartirse entre varios atributos del JSON o entre atributos de los JSON de la Key y el Value. |

Hay algunas variable predefinidas por la aplicación que tiene un significado especial:

| variable | descripción                                                  |
| -------- | ------------------------------------------------------------ |
| $i       | índice relativo empezando por 1 del mensaje cuando se mandan varios mensajes a la vez. Si solo se manda uno será, evidentemente, 1. Se resetea cada vez que se envía una serie de mensajes |
| $counter | contador del número de mensajes enviados desde que se arranca la aplicación |

También es posible asignar valores a variables que nos interese usar en nuestras plantillas. Para ello, en el archivo de propiedades `variables.properties` se debe crear un entrada por cada variable que queramos asignar. Dicha variable luego podrá ser usado como valor en cualquiera de los campos de la plantilla.


A continuación se muestra un ejemplo simple de plantilla

``` 
{
  value: @file,
  flag: @b(true, false, false, false),
  id: @uuid,
  type: @s(A, B, C),
  date: @date(yyyy-MM-dd),
  origin: $origin,
  number_string: @s($i),
  user: @file(users)    
}
```
La plantilla de ejemplo podría generar el JSON:


```
{
  "value": "foo",  
  "flag": false,
  "id": "b3d2cf2d-6267-479c-8df2-4305491537e4",
  "type": "B",
  "date": "2021-02-07",
  "origin" : "FOOBAR",
  "number_string" : "1",
  "user" : "miriam"

}
```

- value: un valor de cadena  a partir de una línea aleatoria del archivo de texto almacenado en la carpeta de recursos de la aplicación `jsontemplate/default.txt`.
- flag: valor booleano con un 75% de probabilidad de que sea false.
- id: un identificador único universal.
- type: una cadena aleatoria de entre A, B y C.
- date: la fecha actual con formato año, mes y día separados por guión.
- origin:  la cadena variable plp.origin definida en el 
- number_string: una cadena con el número de mensaje,
- user: una de las líneas del archivo users.txt

Para una referencia mas exhaustiva se puede consultar el manual y los ejemplos que hay en el mismo repositorio.



## Consumición de mensajes Kafka

![json](./src/main/resources/images/akfak.png?at=refs%2Fheads%2Fdevelop)

La lectura de mensajes del ecosistema Kafka se realiza desde el panel que muestra la pestaña lateral Consumer.

Se debe seleccionar el entorno en el que vamos a operar antes de iniciar la recepción de mensajes.

Para simplificar la localización e un consumidor determinado se puede seleccionar previamente un dominio, de tal manera que las opciones para elegir un consumidor se reducen a los consumidores de mensajes de dicho dominio. Si no se selecciona ningún dominio se puede seleccionar el consumidor genérico.

La lectura de los mensajes se realiza posicionando los offsets de cada partición en el último mensaje recibido y rebobinando un número determinado de mensajes, por defecto 50.

Los mensajes que se lean serán mostrados en una tabla, al seleccionar cada una de las filas se mostrará el contenido de la clave y el valor de cada mensaje en las pestañas `Key` y `Value`.

Es posible filtrar los mensajes que se van a mostrar en la tabla Seleccionado en el combo box o bien la opción `Contiene texto` o la opción `Filtro Javascript`.

### Filtro Contiene texto

Se mostraran los mensajes que contengan un texto determinado en el JSON del key o value. La comparación se hace directamente contra el texto del JSON.

### Filtro Javascript

Para especificar un filtro usando Javascript, se debe introducir en la pestaña `Filtro` un código JavaScript que devuelva true si el mensaje debe ser visualizado en la tabla o false en caso contrario. Para implementar el filtro se pueden usar las variables ya definidas:

| Variable  | Descripción                                                  |
| --------- | ------------------------------------------------------------ |
| key       | objeto javascript que contiene la estructura de datos de la clave del mensaje. |
| value     | objeto javascript que contiene la estructura de datos del valor del mensaje. |
| jsonKey   | string que contiene el JSON de la clave del mensaje.         |
| jsonValue | string que contiene el JSON del valor del mensaje.           |

Ejemplos de filtro:

Supongamos que la clave de los mensajes es del tipo:

````json
{
	"id": "001"
}
````

Y el valor de los mensajes es del tipo:

````json
{
	"name": "pepe",
	"telefono": "666666666",
	"grupo": "A",
    "direcciones": {
        "Oficina": {
			"via": "C/ Principal"            
        }, 
        "Taller": {
            "via": "Carretera N4"
        }
    },
	"facturas": [ {
        	"id": "F001",
			"importe": 100
		}, {
            "id": "F002",
            "importe": 50
        }
	]
}
````



Para filtrar los mensajes del grupo "A" se podría usar el filtro:

```javascript
return value.grupo == 'A';
```



Para filtrar mensajes que tengan alguna dirección con la vía "Carretera N4" :

````javascript
if (!value.direcciones) {
	return false;
}
for (var[nombre, direccion] of Object.entries(value.direcciones)) {
  if (direccion.via == "Carretera N4") {
      return true;
  }
}
return false;
````



Para filtrar los mensajes que tengan facturas cuyo importe sea mayor que 90:

````javascript
return value.facturas && value.facturas.some(factura => factura.importe > 90);
````



Si se quiere implementar con JavaScript un filtro similar al que consigue seleccionando la opción `Contiene texto` se puede hacer de la siguiente forma:

````javascript
return jsonKey.includes("El texto a buscar") || jsonValue.includes("El texto a buscar");
````



### GenericConsumer

Normalmente las clases de los objetos cliente deben ser declarados dentro del código de la aplicación para que estén disponibles los consumidores específicos, sin embargo hay un objeto consumidor genérico que permite leer mensajes creados a partir de contenido *JSON* sin conocimiento de las clases AVRO. Este consumidor se llama `GenericConsumer`.

Para enviar mensajes, en primer lugar, hay que seleccionar el entorno al que van dirigidos los mensajes. 

Luego hay que seleccionar el productor `GenericProducer`.

A continuación, seleccionar o introducir directamente el nombre del *topic* del que se desean leer los mensajes. Seleccionando el consumidor genérico en el combo se muestran todos los *topics* declarados por los consumidores específicos. Pero no hay que limitarse a ese conjunto, por el contrario, se puede usar cualquiera escribiendo su nombre directamente en el campo de edición.



## Schema Registry

![json](./src/main/resources/images/schemaregistry.png?at=refs%2Fheads%2Fdevelop)

La interacción con el Schema Registry se hace desde el panel que muestra la pestaña lateral titulada Schema.

Este panel permite consultar los esquemas registrados en un sujeto determinado. Cada *topic* consta de dos sujetos, uno cuyo nombre termina en `-key` para la KEY y otro terminado en `-value` para el VALUE.

Para obtener todas las versiones de esquemas que contiene un sujeto se debe selecciona el entorno y el sujeto en los combo box correspondientes. A continuación pulsar el botón `Obtener esquemas`

Se puede seleccionar previamente un dominio para facilitar encontrar el sujeto mas fácilmente.

Una vez se hayan presentado las versiones en la lista se puede seleccionar una de ellas para que se muestre en la pestaña `Esquema` el contenido del esquema.

Para borrar o comparar una  versión determinada de un esquema con su versión anterior de debe pulsar botón derecho encima de la versión en la lista de versiones. Esto  mostrará un menú contextual desde el que ordenar dichas acciones.



## Generación de JSON, esquemas y plantillas

![json](./src/main/resources/images/json.png?at=refs%2Fheads%2Fdevelop)

La generación de contenido JSON se realiza desde el panel que muestra la pestaña lateral titulada *JSON*.

Desde esta panel se puede generar contenido JSON de prueba de cualquier clase que esté accesible por la aplicación siempre que sea una clase AVRO o JavaBean.

Hay una selección de clases que se muestran en el combo box para facilitar la búsqueda. Las clases que aparecen son las que coinciden con alguna de las expresiones indicadas en el archivo `classes.txt` dentro de la carpeta de recursos.

Para facilitar la búsqueda de clases, dentro de las que están disponibles en el combo box, se puede usar el filtro teniendo en cuenta que el nombre completo de la clases que se muestren como opciones en el combo box serán las que contengan cada una de las palabras que se indiquen en el combo sin tener que ser de forma consecutiva.

Es posible seleccionar el nombre de la clase o escribirla, directamente, en el editor del combo box.

Una vez seleccionada la clase se creará, en profundidad, una instancia con valores de ejemplo de dicha clase. Si se detecta recursividad (clases que se contengan a sí mismas o a clases en las cuales están contenidas,  se dejan de generar valores para no entrar en un bucle infinito).

A partir de la instancia generada se genera un contenido JSON que se mostrará en la pestaña correspondiente. 

Además se mostrará el esquema AVRO (si es una clase AVRO generada) en otra de las pestañas.

Por ultimo se mostrará en la última pestana una plantilla generada a partir del JSON de ejemplo. El JSON y la plantilla pueden servir para, una vez editadas adecuadamente, usarse como ejemplos de prueba que pueden ser añadidos a los recursos.



## Funciones comunes en los editores

### Búsqueda de texto

En la parte inferior de la ventana hay un campo de edición que permite la búsqueda de texto en los editores JSON, de momento no funciona en las consolas de información. Al editar este campo se va seleccionando las ocurrencias del texto que hayamos editado. Al pulsar la tecla `Intro` se activa el foco del editor.

También se puede iniciar una búsqueda seleccionando un texto del editor y pulsando `CTRL-F3`. Luego al pulsar `F3` el cursor se irá desplazando a la siguiente ocurrencia y al pulsar `SHIFT-F3` a la ocurrencia anterior.

### Limpieza de editores y consolas

Para limpiar, con un solo toque de ratón, el texto de los editores o consolar se puede usar el botón ![Limpiar](./src/main/resources/images/rubber.png?at=refs%2Fheads%2Fdevelop). 

### Copiar al portapapeles

El botón ![copiar al portapapeles](./src/main/resources/images/copy.png?at=refs%2Fheads%2Fdevelop) permite, de forma cómoda, copiar al portapapeles el texto completo del editor que se esté mostrando en un momento determinado.

### Formatear JSON

Para formatear el contenido JSON en cualquiera de los editores se debe usar la combinación de teclado `CRL` `ALT` `L`.



