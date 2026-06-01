package com.example.ui.models

import java.util.UUID

enum class BodyView {
    FRONT, BACK, ORGANS
}

enum class BodySystem {
    EXTERNAL, SKELETAL, ORGANS
}

data class BodyPart(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val system: BodySystem,
    val view: BodyView,
    val xPercent: Float, // 0.0f to 1.0f on the canvas representation
    val yPercent: Float, // 0.0f to 1.0f on the canvas representation
    val description: String,
    val mnemonic: String = "",
    val function: String = "",
    val isCustom: Boolean = false
)

data class StudyProgress(
    val partId: String,
    val timesReviewed: Int = 0,
    val timesCorrect: Int = 0,
    val timesIncorrect: Int = 0,
    val levelOfMastery: Int = 0 // 0 to 5 space repetition level
) {
    val accuracy: Float
        get() = if (timesReviewed == 0) 0f else (timesCorrect.toFloat() / timesReviewed.toFloat()) * 100f
}

object PredefinedData {
    val bodyParts = listOf(
        // === SISTEMA EXTERNAL (VISTA FRONTAL) ===
        BodyPart(
            id = "ext_cabeza",
            name = "Cabeza",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.50f,
            yPercent = 0.07f,
            description = "Estructura superior que contiene el cerebro, los ojos, oídos, nariz y boca.",
            function = "Procesamiento sensorial central y protección del sistema nervioso superior.",
            mnemonic = "La torre de control de todo el organismo."
        ),
        BodyPart(
            id = "ext_cuello",
            name = "Cuello",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.50f,
            yPercent = 0.14f,
            description = "Región de conexión entre la cabeza y el torso, por donde pasan vasos y nervios principales.",
            function = "Soporte de la cabeza y vía de paso para la tráquea y el esófago.",
            mnemonic = "El puente conductor de las señales de vida."
        ),
        BodyPart(
            id = "ext_hombro_der",
            name = "Hombro Derecho",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.38f,
            yPercent = 0.19f,
            description = "Complejo articular superior derecho que une el brazo con el tórax.",
            function = "Permite un amplio rango de movimiento biomecánico del miembro superior.",
            mnemonic = "La articulación de mayor rotación y alcance de tu brazo."
        ),
        BodyPart(
            id = "ext_hombro_izq",
            name = "Hombro Izquierdo",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.62f,
            yPercent = 0.19f,
            description = "Complejo articular superior izquierdo que une el brazo con el tórax.",
            function = "Permite un amplio plano de movimientos del miembro superior izquierdo.",
            mnemonic = "Eje del hombro que facilita levantar y cargar pesos."
        ),
        BodyPart(
            id = "ext_pecho",
            name = "Pecho (Pectoral)",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.50f,
            yPercent = 0.24f,
            description = "Parte frontal superior del tórax, cubierta por los músculos pectorales.",
            function = "Protección de órganos vitales e intervención en movimientos del brazo.",
            mnemonic = "El escudo del corazón."
        ),
        BodyPart(
            id = "ext_abdomen",
            name = "Abdomen",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.50f,
            yPercent = 0.37f,
            description = "Zona media del torso, aloja la mayoría de los órganos digestivos primarios.",
            function = "Presión intraabdominal, respiración y estabilidad de la columna.",
            mnemonic = "El centro de fuerza del núcleo central (core)."
        ),
        BodyPart(
            id = "ext_antebrazo_der",
            name = "Antebrazo Derecho",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.33f,
            yPercent = 0.40f,
            description = "Región del brazo derecho entre el codo y la muñeca.",
            function = "Controla movimientos finos de rotación de la mano y flexión de dedos.",
            mnemonic = "El motor del agarre derecho."
        ),
        BodyPart(
            id = "ext_antebrazo_izq",
            name = "Antebrazo Izquierdo",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.67f,
            yPercent = 0.40f,
            description = "Región del brazo izquierdo entre el codo y la muñeca.",
            function = "Rotación de la mano izquierda y agarre mecánico.",
            mnemonic = "El motor del agarre izquierdo."
        ),
        BodyPart(
            id = "ext_mano_der",
            name = "Mano Derecha",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.28f,
            yPercent = 0.49f,
            description = "Extremidad superior derecha con 5 dígitos y alta movilidad.",
            function = "Prensión, tacto detallado y manipulación precisa del entorno.",
            mnemonic = "La herramienta de creación primordial externa."
        ),
        BodyPart(
            id = "ext_mano_izq",
            name = "Mano Izquierda",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.72f,
            yPercent = 0.49f,
            description = "Extremidad superior izquierda con 5 dígitos para manipulación fina.",
            function = "Prensión y soporte mecánico de precisión.",
            mnemonic = "Nuestra pinza de agarre sensorial izquierda."
        ),
        BodyPart(
            id = "ext_muslo_der",
            name = "Muslo Derecho",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.43f,
            yPercent = 0.58f,
            description = "Región del miembro inferior derecho entre la cadera y la rodilla.",
            function = "Genera la fuerza para caminar, correr y saltar merced a los cuádriceps.",
            mnemonic = "La batería de poder para impulsarte hacia adelante."
        ),
        BodyPart(
            id = "ext_muslo_izq",
            name = "Muslo Izquierdo",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.57f,
            yPercent = 0.58f,
            description = "Región del miembro inferior izquierdo entre la cadera y la rodilla.",
            function = "Fuerza motora de la pierna izquierda.",
            mnemonic = "Pilar muscular del muslo."
        ),
        BodyPart(
            id = "ext_rodilla_der",
            name = "Rodilla Derecha",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.43f,
            yPercent = 0.72f,
            description = "Articulación compleja de flexoextensión de la pierna derecha.",
            function = "Amortiguación de impactos corporales y soporte en la bipedestación.",
            mnemonic = "El resorte articulado de tu paso derecho."
        ),
        BodyPart(
            id = "ext_rodilla_izq",
            name = "Rodilla Izquierda",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.57f,
            yPercent = 0.72f,
            description = "Articulación de flexoextensión de la pierna izquierda.",
            function = "Amortiguación y distribución del peso dinámico de la marcha.",
            mnemonic = "El amortiguador dinámico izquierdo."
        ),
        BodyPart(
            id = "ext_pie_der",
            name = "Pie Derecho",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.41f,
            yPercent = 0.94f,
            description = "Estructura terminal de apoyo biomecánico con arcos flexibles.",
            function = "Distribución del peso corporal y propulsión mecánica en la marcha.",
            mnemonic = "El cimiento que conecta tu cuerpo con la tierra."
        ),
        BodyPart(
            id = "ext_pie_izq",
            name = "Pie Izquierdo",
            system = BodySystem.EXTERNAL,
            view = BodyView.FRONT,
            xPercent = 0.59f,
            yPercent = 0.94f,
            description = "Estructura terminal de apoyo izquierdo.",
            function = "Soporte de la postura y equilibrio estático y dinámico.",
            mnemonic = "El anclaje izquierdo para el balance total."
        ),

        // === SISTEMA EXTERNAL (VISTA POSTERIOR) ===
        BodyPart(
            id = "ext_nuca",
            name = "Nuca",
            system = BodySystem.EXTERNAL,
            view = BodyView.BACK,
            xPercent = 0.50f,
            yPercent = 0.12f,
            description = "Parte posterior del cuello, rica en importantes inserciones musculares cervicales.",
            function = "Permite la extensión y rotación de la cabeza.",
            mnemonic = "El protector del tallo cerebral."
        ),
        BodyPart(
            id = "ext_espalda_alta",
            name = "Espalda Alta",
            system = BodySystem.EXTERNAL,
            view = BodyView.BACK,
            xPercent = 0.50f,
            yPercent = 0.23f,
            description = "Región dorsal que abarca el trapecio, dorsales y zona interescapular.",
            function = "Soporte escapular, tracción y enderezamiento corporal.",
            mnemonic = "El soporte de carga que mantiene el cuerpo erguido."
        ),
        BodyPart(
            id = "ext_lumbar",
            name = "Espalda Baja (Lumbar)",
            system = BodySystem.EXTERNAL,
            view = BodyView.BACK,
            xPercent = 0.50f,
            yPercent = 0.41f,
            description = "Región lumbar inferior de la espalda sometida a grandes cargas de palanca.",
            function = "Soporta el torso superior y facilita movimientos de flexión dorsal.",
            mnemonic = "La zona de transferencia del peso superior a las piernas."
        ),
        BodyPart(
            id = "ext_gluteo",
            name = "Glúteo",
            system = BodySystem.EXTERNAL,
            view = BodyView.BACK,
            xPercent = 0.50f,
            yPercent = 0.53f,
            description = "Músculos glúteos de la región posterior de la cadera, los más potentes del cuerpo.",
            function = "Extensión y abducción de la cadera, vital para mantenerse erguido.",
            mnemonic = "Glúteos fuertes impiden el dolor de espalda baja."
        ),
        BodyPart(
            id = "ext_gemelo_der",
            name = "Gemelo Derecho (Gastrocnemio)",
            system = BodySystem.EXTERNAL,
            view = BodyView.BACK,
            xPercent = 0.42f,
            yPercent = 0.81f,
            description = "Músculo de doble vientre en la pantorrilla derecha.",
            function = "Flexión plantar del pie y elevación del cuerpo en puntas.",
            mnemonic = "El propulsor de salto en tu pierna derecha."
        ),
        BodyPart(
            id = "ext_gemelo_izq",
            name = "Gemelo Izquierdo (Gastrocnemio)",
            system = BodySystem.EXTERNAL,
            view = BodyView.BACK,
            xPercent = 0.58f,
            yPercent = 0.81f,
            description = "Músculo de doble vientre en la pantorrilla izquierda.",
            function = "Flexión plantar y propulsión izquierda.",
            mnemonic = "La caldera de impulso del paso izquierdo."
        ),

        // === SISTEMA ÓSEO (SKELETAL - VISTA FRONTAL) ===
        BodyPart(
            id = "sk_craneo",
            name = "Cráneo",
            system = BodySystem.SKELETAL,
            view = BodyView.FRONT,
            xPercent = 0.50f,
            yPercent = 0.06f,
            description = "Conjunto de huesos planos soldados que forman la cavidad craneal.",
            function = "Protección física del encéfalo y soporte rígido facial.",
            mnemonic = "El cofre rígido del pensamiento."
        ),
        BodyPart(
            id = "sk_mandibula",
            name = "Mandíbula",
            system = BodySystem.SKELETAL,
            view = BodyView.FRONT,
            xPercent = 0.50f,
            yPercent = 0.11f,
            description = "Hueso maxilar inferior móvil de la cara.",
            function = "Masticación, gesticulación y soporte del plano dental inferior.",
            mnemonic = "El único hueso móvil del cráneo."
        ),
        BodyPart(
            id = "sk_clavicula",
            name = "Clavícula",
            system = BodySystem.SKELETAL,
            view = BodyView.FRONT,
            xPercent = 0.58f,
            yPercent = 0.17f,
            description = "Hueso largo con forma de 'S' que une el esternón con la escápula.",
            function = "Puntal para la articulación del hombro, distribuyendo fuerzas mecánicas.",
            mnemonic = "La llave (clavis) que sostiene tus brazos colgados."
        ),
        BodyPart(
            id = "sk_esternon",
            name = "Esternón",
            system = BodySystem.SKELETAL,
            view = BodyView.FRONT,
            xPercent = 0.50f,
            yPercent = 0.25f,
            description = "Hueso plano e impar situado en la parte anterior central del tórax.",
            function = "Punto de anclaje de las costillas y protección del mediastino.",
            mnemonic = "La coraza del centro de tu pecho."
        ),
        BodyPart(
            id = "sk_costillas",
            name = "Costillas",
            system = BodySystem.SKELETAL,
            view = BodyView.FRONT,
            xPercent = 0.44f,
            yPercent = 0.30f,
            description = "Doce pares de huesos curvados y elásticos que forman la caja torácica.",
            function = "Protección intratorácica y posibilitar la expansión elástica respiratoria.",
            mnemonic = "Los arcos de la jaula protectora respiratoria."
        ),
        BodyPart(
            id = "sk_humero",
            name = "Húmero",
            system = BodySystem.SKELETAL,
            view = BodyView.FRONT,
            xPercent = 0.35f,
            yPercent = 0.30f,
            description = "Hueso largo del brazo, se articula con la escápula y con el codo.",
            function = "Transmisión de fuerza del hombro al antebrazo para palancas complejas.",
            mnemonic = "El hueso principal del brazo."
        ),
        BodyPart(
            id = "sk_vertebra",
            name = "Columna Vertebral",
            system = BodySystem.SKELETAL,
            view = BodyView.FRONT,
            xPercent = 0.50f,
            yPercent = 0.34f,
            description = "Columna de 33 vértebras (cervicales, torácicas, lumbares, sacras, coxígeas).",
            function = "Protección de la médula espinal y soporte del eje estático del esqueleto.",
            mnemonic = "El pilar central flexible de la estructura humana."
        ),
        BodyPart(
            id = "sk_pelvis",
            name = "Pelvis (Hueso Coxal)",
            system = BodySystem.SKELETAL,
            view = BodyView.FRONT,
            xPercent = 0.50f,
            yPercent = 0.48f,
            description = "Estructura ósea en forma de embudo que conecta el tronco con las piernas.",
            function = "Soporte de órganos internos y transmisión del peso a la cadera.",
            mnemonic = "La gran cuenca que asienta la base de tu torso."
        ),
        BodyPart(
            id = "sk_radio",
            name = "Radio",
            system = BodySystem.SKELETAL,
            view = BodyView.FRONT,
            xPercent = 0.31f,
            yPercent = 0.42f,
            description = "Hueso lateral del antebrazo, alineado con el pulgar.",
            function = "Eje sobre el cual rotamos la palma de la mano (pronosupinación).",
            mnemonic = "El radio da vueltas alrededor del cúbito como una antena."
        ),
        BodyPart(
            id = "sk_cubito",
            name = "Cúbito (Ulna)",
            system = BodySystem.SKELETAL,
            view = BodyView.FRONT,
            xPercent = 0.28f,
            yPercent = 0.42f,
            description = "Hueso medial del antebrazo, en paralelo al radio pero fijo en el codo.",
            function = "Bisagra primordial de flexión-extensión del brazo en el codo.",
            mnemonic = "El cimiento estable de tu articulación del codo."
        ),
        BodyPart(
            id = "sk_femur",
            name = "Fémur",
            system = BodySystem.SKELETAL,
            view = BodyView.FRONT,
            xPercent = 0.42f,
            yPercent = 0.60f,
            description = "Hueso largo del muslo, el mayor del esqueleto.",
            function = "Suporte estático titánico de carga corporal y palanca biomecánica motora.",
            mnemonic = "Fémur: Fuerza Férrea que te sostiene de pie."
        ),
        BodyPart(
            id = "sk_rotula",
            name = "Rótula (Patela)",
            system = BodySystem.SKELETAL,
            view = BodyView.FRONT,
            xPercent = 0.42f,
            yPercent = 0.72f,
            description = "Hueso sesamoideo plano situado en la parte anterior de la rodilla.",
            function = "Multiplica la fuerza de tracción del tendón del cuádriceps como una polea.",
            mnemonic = "La polea des deslizante que levanta tus piernas."
        ),
        BodyPart(
            id = "sk_tibia",
            name = "Tibia",
            system = BodySystem.SKELETAL,
            view = BodyView.FRONT,
            xPercent = 0.41f,
            yPercent = 0.83f,
            description = "Hueso interno y más grueso de la pierna.",
            function = "Soporta todo el peso transmitido desde el fémur hacia el tobillo.",
            mnemonic = "Tibia es la 'T' de Tronco que sostiene todo el peso de la pantorrilla."
        ),
        BodyPart(
            id = "sk_perone",
            name = "Peroné (Fíbula)",
            system = BodySystem.SKELETAL,
            view = BodyView.FRONT,
            xPercent = 0.44f,
            yPercent = 0.83f,
            description = "Hueso externo y delgado de la pierna, paralelo a la tibia.",
            function = "Añade estabilidad exterior en el tobillo y sirve de inserción de músculos laterales.",
            mnemonic = "El soporte fino y elástico en el contorno externo."
        ),

        // === SISTEMA ÓRGANOS (ORGAN VIEW - VISTA DE ÓRGANOS) ===
        BodyPart(
            id = "org_cerebro",
            name = "Cerebro",
            system = BodySystem.ORGANS,
            view = BodyView.ORGANS,
            xPercent = 0.50f,
            yPercent = 0.05f,
            description = "Órgano maestro del sistema límbico y cognitivo cerebral.",
            function = "Integra datos, evoca ideas, modula emociones y la conciencia.",
            mnemonic = "El procesador inteligente de dos hemisferios."
        ),
        BodyPart(
            id = "org_pulmon_der",
            name = "Pulmón Derecho",
            system = BodySystem.ORGANS,
            view = BodyView.ORGANS,
            xPercent = 0.43f,
            yPercent = 0.25f,
            description = "Órgano respiratorio derecho con tres lóbulos pulmonares.",
            function = "Oxigenación celular directa de la circulación menor y descarte de CO2.",
            mnemonic = "El pulmón derecho es más ancho por dejar espacio al hígado inferior."
        ),
        BodyPart(
            id = "org_pulmon_izq",
            name = "Pulmón Izquierdo",
            system = BodySystem.ORGANS,
            view = BodyView.ORGANS,
            xPercent = 0.57f,
            yPercent = 0.25f,
            description = "Órgano respiratorio izquierdo, con dos lóbulos.",
            function = "Hematosis e intercambio de aire.",
            mnemonic = "Ligeramente más chico para dar asilo del latido cardíaco."
        ),
        BodyPart(
            id = "org_corazon",
            name = "Corazón",
            system = BodySystem.ORGANS,
            view = BodyView.ORGANS,
            xPercent = 0.51f,
            yPercent = 0.26f,
            description = "Bomba de cavidades musculares (aurículas y ventrículos).",
            function = "Presuriza y mantiene en perpetuo flujo el caudal sanguíneo total.",
            mnemonic = "El motor del puño cerrado en el centro-izquierdo del tórax."
        ),
        BodyPart(
            id = "org_higado",
            name = "Hígado",
            system = BodySystem.ORGANS,
            view = BodyView.ORGANS,
            xPercent = 0.45f,
            yPercent = 0.35f,
            description = "La glándula metabólica más grande e importante del abdomen derecho.",
            function = "Desintoxicación de la sangre, síntesis de proteínas y secreción de bilis.",
            mnemonic = "El laboratorio bioquímico del cuerpo, capaz de autoregenerarse."
        ),
        BodyPart(
            id = "org_estomago",
            name = "Estómago",
            system = BodySystem.ORGANS,
            view = BodyView.ORGANS,
            xPercent = 0.54f,
            yPercent = 0.36f,
            description = "Órgano del tracto superior en forma de saco muscular elástico.",
            function = "Licúa los alimentos con ácidos y los prepara para el duodeno.",
            mnemonic = "Un almacén ácido que ruge cuando está hambriento."
        ),
        BodyPart(
            id = "org_rinon_der",
            name = "Riñón Derecho",
            system = BodySystem.ORGANS,
            view = BodyView.ORGANS,
            xPercent = 0.44f,
            yPercent = 0.42f,
            description = "Filtro depurador retroperitoneal de forma de judía o habichuela.",
            function = "Filtra la urea sanguínea, controla la hidratación y presión arterial.",
            mnemonic = "El purificador inteligente de fluidos."
        ),
        BodyPart(
            id = "org_rinon_izq",
            name = "Riñón Izquierdo",
            system = BodySystem.ORGANS,
            view = BodyView.ORGANS,
            xPercent = 0.56f,
            yPercent = 0.42f,
            description = "Filtro depurador retroperitoneal izquierdo.",
            function = "Osmorregulación y formación de la orina.",
            mnemonic = "Un poco más alto que su contraparte derecha."
        ),
        BodyPart(
            id = "org_intestino_gro",
            name = "Intestino Grueso",
            system = BodySystem.ORGANS,
            view = BodyView.ORGANS,
            xPercent = 0.50f,
            yPercent = 0.47f,
            description = "Tramo terminal del tubo digestivo compuesto por el colon y ciego.",
            function = "Reabsorción crítica de agua y consolidación del bolo fecal.",
            mnemonic = "El recuperador de líquidos de la digestión."
        ),
        BodyPart(
            id = "org_vejiga",
            name = "Vejiga",
            system = BodySystem.ORGANS,
            view = BodyView.ORGANS,
            xPercent = 0.50f,
            yPercent = 0.54f,
            description = "Reservorio muscular elástico de la pelvis para alojar la orina antes de excretarla.",
            function = "Contención y posterior vaciado regulado del líquido residual.",
            mnemonic = "El globo de control hídrico terminal."
        )
    )
}
