data class Automato (val inicio: Estado = Estado()){

    fun getEstados() : MutableList<Estado> {
        val estados:MutableList<Estado> = mutableListOf(inicio)

        var cont = 0
        while(cont < estados.size){
            val estado = estados[cont]
            for(par in estado.proximos){
                if(!estados.contains(par.second)){
                    estados.add(par.second)
                }
            }

            cont++
        }

        return estados
    }

    fun getEstadosFinais() : MutableList<Estado> {
        val finais = mutableListOf(inicio)

        var cont = 0
        while(cont < finais.size){
            val estado = finais[cont]
            for(par in estado.proximos){
                if(par.second.isFinal() && !finais.contains(par.second)){
                    finais.add(par.second)
                }
            }

            cont++
        }

        return finais
    }
}

data class Estado (val proximos: MutableList<Pair<String?, Estado>> = mutableListOf(), private val id:Int = getIndex()){


    fun isFinal() = proximos.isEmpty()

    companion object {
        private var cont = 0
        fun getIndex() = cont++
    }

    fun estadoMaisProximoCom(string: String, visitados: MutableList<Estado> = mutableListOf()): Estado?{
        for(par in proximos){
            val stringPar = par.first
            val estado = par.second
            if(stringPar == null){
                if(estado !in visitados){
                    val maisProximo = estado.estadoMaisProximoCom(string, visitados)
                    if(maisProximo != null){
                        return maisProximo
                    }
                }
            }else if(stringPar == string){
                return estado
            }
        }

        return null
    }

    override fun equals(other: Any?): Boolean {
        return other is Estado && other.id == id
    }

    override fun hashCode(): Int {
        var result = proximos.hashCode()
        result = 31 * result + id
        return result
    }
}

fun convertRHSToAutomato(rhs: RHS) : Automato {
    return when(rhs){
        is Identifier -> {
            Automato()
        }

        is Terminal -> {
            Automato(Estado(mutableListOf(Pair(rhs.name, Estado()))))
        }

        is Loop -> {
            convertLoopToAutomato(rhs)
        }

        is Or -> {
            convertOrToAutomato(rhs)
        }

        is Concat -> {
            convertConcatToAutomato(rhs)
        }

        else -> Automato()
    }
}

fun convertLoopToAutomato(loop: Loop) : Automato {

    val fim = Estado()
    val automatoLoop = convertRHSToAutomato(loop.item)
    for (estado in automatoLoop.getEstadosFinais()){
        estado.proximos.add(Pair(null, automatoLoop.inicio))
        estado.proximos.add(Pair(null, fim))
    }
    val inicio = Estado()
    inicio.proximos.add(Pair(null, automatoLoop.inicio))
    inicio.proximos.add(Pair(null, fim))

    return Automato(inicio)
}

fun convertOrToAutomato(or: Or) : Automato {

    val fim = Estado()

    val automatoLeft = convertRHSToAutomato(or.left)
    for (estado in automatoLeft.getEstadosFinais()){
        estado.proximos.add(Pair(null, fim))
    }

    val automatoRight = convertRHSToAutomato(or.right)
    for (estado in automatoRight.getEstadosFinais()){
        estado.proximos.add(Pair(null, fim))
    }

    val inicio = Estado()
    inicio.proximos.add(Pair(null, automatoLeft.inicio))
    inicio.proximos.add(Pair(null, automatoRight.inicio))

    return Automato(inicio)
}

fun convertConcatToAutomato(concat: Concat) : Automato {

    val fim = Estado()

    val automatoRight = convertRHSToAutomato(concat.right)
    for (estado in automatoRight.getEstadosFinais()){
        estado.proximos.add(Pair(null, fim))
    }

    val automatoLeft = convertRHSToAutomato(concat.left)
    for (estado in automatoLeft.getEstadosFinais()){
        estado.proximos.add(Pair(null, automatoRight.inicio))
    }

    return Automato(automatoLeft.inicio)
}

fun convertNFAtoDFA(automato: Automato) : HashMap<Pair<Int, String>, Int> {
    val estados = automato.getEstados()
    val alfabeto = mutableListOf<String>()

    for(estado in estados){
        for(par in estado.proximos){
            val s = par.first
            if(s != null && !alfabeto.contains(s)){
                alfabeto.add(s)
            }
        }
    }

    val map: HashMap<Pair<Int, String>, Int> = hashMapOf()

    for(index in 0 until estados.size){
        val estado = estados[index]
        for(string in alfabeto){
            val estadoMaisProximo = estado.estadoMaisProximoCom(string)
            val indexProximo = estados.indexOf(estadoMaisProximo)
            map[Pair(index, string)] = indexProximo
        }
    }

    return map
}