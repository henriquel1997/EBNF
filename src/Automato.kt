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
        val naoVisitados = mutableListOf(inicio)
        val finais = mutableListOf<Estado>()

        var cont = 0
        while(cont < naoVisitados.size){
            val estado = naoVisitados[cont]
            if(estado.isFinal()){
                finais.add(estado)
            }

            for(par in estado.proximos){
                if(!naoVisitados.contains(par.second)){
                    naoVisitados.add(par.second)
                }
            }

            cont++
        }

        return finais
    }
}

data class Estado (val proximos: MutableList<Pair<Char?, Estado>> = mutableListOf(), val id:Int = getIndex()){


    fun isFinal() = proximos.isEmpty()

    companion object {
        private var cont = 0
        fun getIndex() = cont++
    }

    fun eClosure() : MutableList<Estado>{
        val estados = mutableListOf(this)

        var index = 0
        while(index < estados.size){
            val estado = estados[index]
            for(par in estado.proximos){
                if(par.first == null && !estados.contains(par.second)){
                    estados.add(par.second)
                }
            }

            index++
        }

        estados.sortBy { it.id }

        return estados
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

data class TabelaTransicao(val map: HashMap<Pair<Int, Char>, Int>, val finais: List<Int>)

fun convertRHSToAutomato(rhs: RHS) : Automato {
    return when(rhs){
        is Identifier -> {
            Automato()
        }

        is Terminal -> {
            Automato(Estado(mutableListOf(Pair(rhs.letter, Estado()))))
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

fun convertENFAtoDFA(automato: Automato) : TabelaTransicao {

    val estados = automato.getEstados()
    val alfabeto = mutableListOf<Char>()

    for(estado in estados){
        for(par in estado.proximos){
            val c = par.first
            if(c != null && !alfabeto.contains(c)){
                alfabeto.add(c)
            }
        }
    }

    val map: HashMap<Pair<Int, Char>, Int> = hashMapOf()
    val closures = mutableListOf(estados[0].eClosure())

    val getIndexClosure = check@{ novaClosure: MutableList<Estado> ->
        for(indexClosure in 0 until closures.size){
            val closure = closures[indexClosure]

            if(closure.size == novaClosure.size){
                var igual = true
                for(index in 0 until closure.size){
                    if(closure[index].id != novaClosure[index].id){
                        igual = false
                        break
                    }
                }

                if(igual){
                    return@check indexClosure
                }
            }

        }

        return@check -1
    }

    var index = 0
    while(index < closures.size){
        val closure = closures[index]

        for(char in alfabeto){
            val estadosParaClosure = mutableListOf<Estado>()
            for(estado in closure){
                for(prox in estado.proximos){
                    if(prox.first == char && !estadosParaClosure.contains(prox.second)){
                        estadosParaClosure.add(prox.second)
                    }
                }
            }

            val novaClosure = mutableListOf<Estado>()

            for(estado in estadosParaClosure){
                for(estadoClosure in estado.eClosure()){
                    if(!novaClosure.contains(estadoClosure)){
                        novaClosure.add(estadoClosure)
                    }
                }
            }

            map[Pair(index, char)] = if(novaClosure.isNotEmpty()) {

                novaClosure.sortBy { it.id }

                val id = getIndexClosure(novaClosure)
                if(id < 0) {
                    closures.add(novaClosure)
                    closures.size - 1
                } else {
                    id
                }
            } else {
                -1
            }
        }

        index++
    }

    val finais = mutableListOf<Int>()
    val finaisAutomato = automato.getEstadosFinais()

    for(i in 0 until closures.size){
        val closure = closures[i]
        for(estado in closure){
            if(finaisAutomato.contains(estado)){
                finais.add(i)
                break
            }
        }
    }

    return TabelaTransicao(map, finais)
}

fun executaTabelaTransicao(string: String, tabelaTransicao: TabelaTransicao) : Boolean{
    var estado: Int? = 0
    var reconheceu = true
    for(i in 0 until string.length){
        val letra = string[i]
        estado = tabelaTransicao.map[Pair(estado, letra)]

        if(estado == null || estado == -1){
            reconheceu = false
            break
        }

    }

    return reconheceu && tabelaTransicao.finais.contains(estado)
}