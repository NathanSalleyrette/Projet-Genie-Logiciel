#! /bin/sh

# Auteur : lucie
# Version initiale : 12/01/2021

# Generation automatique des tests pour les opération binaires (+ / % * -)
cd "$(dirname "$0")"/../../.. || exit 1

PATH=./src/test/script/launchers:"$PATH"
chemin="./src/test/deca/context/invalid/created/binary_exp/op_comp"


gen_exp_part () {
	args1=$1
	args2=$2
	args3=$3
	args4=$4
	filename="$chemin/${args1}_${args2}_${args3}.deca"
	touch  $filename
	exec 3> $filename
	echo "// Description :" >&3
	echo "//    test $args1 entre un $args2 et un $args3" >&3
	echo "//" >&3
	echo "// Resultats :" >&3
	echo "//		Erreur contextuelle" >&3
	echo "//    	Ligne 11: (3.33) Opérandes de type $args2, $args3, attendu: ‘int’ ou ‘float’" >&3
	echo "">&3
	echo "{" >&3
	echo "$args2 a;" >&3
	echo "$args3 b;" >&3
	echo "boolean c = a ""$args4"" b;" >&3
	echo "}" >&3
	exec 3>&-
}

gen_exp() {
	nom=$1
	op=$2
	gen_exp_part $nom "boolean" "boolean" "$op"
	gen_exp_part $nom "boolean" "float" "$op"
	gen_exp_part $nom "boolean" "int" "$op"
	gen_exp_part $nom "float" "boolean" "$op"
	gen_exp_part $nom "int" "boolean" "$op"
}


gen_exp "lt" "<"
gen_exp "gt" ">"
gen_exp "leq" "<="
gen_exp "geq" ">="
