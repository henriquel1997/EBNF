fun main() {

    //TODO: Não está fazendo a tabela corretamente para essa regra
    val ruleString = "letter = \"A\" | {\"A\" , \"B\"};"
    val beginTime = System.currentTimeMillis()
    val rule = checkRule(ruleString.replace("\\s".toRegex(), ""))
    if(rule != null){
        val automato = convertRHSToAutomato(rule.rhs)
        val tabela = convertENFAtoDFA(automato)
        val string = "ABAB"

        println("String $string reconhecida: "+ executaTabelaTransicao(string, tabela))
    }

    val endTime = System.currentTimeMillis()

    println("$ruleString : is rule: $rule")
    println("Tempo: "+ (endTime - beginTime) + "ms")
}