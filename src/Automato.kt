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

    fun estadosAlcancaveis() : MutableList<Estado>{
        val estados = mutableListOf(this)

        var index = 0
        while(index < estados.size){
            val estado = estados[index]
            for(par in estado.proximos){
                val alcancavel = (par.first == null)
                if(alcancavel){
                    var naoFoiVisitado = true
                    for(e in estados){
                        if(e.id == par.second.id){
                            naoFoiVisitado = false
                            break
                        }
                    }
                    if(naoFoiVisitado){
                        estados.add(par.second)
                    }
                }
            }

            index++
        }

        return estados
    }

    fun estadoMaisProximoCom(char: Char): Estado?{
        val alcancaveis = estadosAlcancaveis()
        for(estado in alcancaveis){
            for(par in estado.proximos){
                if(par.first == char){
                    return par.second
                }
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

    for(index in 0 until estados.size){
        val estado = estados[index]
        for(char in alfabeto){
            val estadoMaisProximo = estado.estadoMaisProximoCom(char)
            val indexProximo = estados.indexOf(estadoMaisProximo)
            map[Pair(index, char)] = indexProximo
        }
    }

    val finais = mutableListOf<Int>()
    val finaisAutomato = automato.getEstadosFinais()

    for(index in 0 until estados.size){
        val estado = estados[index]
        for(e in estado.estadosAlcancaveis()){
            if(finaisAutomato.contains(e)){
                finais.add(index)
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