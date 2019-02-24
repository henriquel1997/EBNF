fun main() {

    val ruleString = "letter = {\"A\" , \"B\"};"
    val beginTime = System.currentTimeMillis()
    val rule = checkRule(ruleString.replace("\\s".toRegex(), ""))
    if(rule != null){
        val automato = convertRHSToAutomato(rule.rhs)
        val tabela = convertENFAtoDFA(automato)
        val string = "ABABABABA"

        println("String $string reconhecida: "+ executaTabelaTransicao(string, tabela))
    }

    val endTime = System.currentTimeMillis()

    println("$ruleString : is rule: $rule")
    println("Tempo: "+ (endTime - beginTime) + "ms")
}