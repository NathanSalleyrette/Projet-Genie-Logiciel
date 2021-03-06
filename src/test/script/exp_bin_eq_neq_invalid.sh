#! /bin/sh

# Auteur : lucie
# Version initiale : 12/01/2021

# Generation automatique des tests pour les opération binaires (< <= > >=)
cd "$(dirname "$0")"/../../.. || exit 1

PATH=./src/test/script/launchers:"$PATH"
chemin="./src/test/deca/context/invalid/created/binary_exp/op_comp"


gen_exp_part () {
	args1=$1
	args2=$2
	args3=$3
	args4=$4
	args5=$5
	filename="$chemin/${args1}_${args2}_${args3}.deca"
	touch  $filename
	exec 3> $filename
	echo "// Description :" >&3
	echo "//    test $args1 entre un $args2 et un $args3" >&3
	echo "//" >&3
	echo "// Resultats :" >&3
	echo "//		Erreur contextuelle" >&3
	echo "//    	Ligne 11: (3.33) Opérandes de type $args2, $args3, attendu: (int|float, int|float) ou (class|null, class|null) ou (bool, bool)" >&3
	echo "$args5">&3
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
	gen_exp_part $nom "boolean" "float" "$op" ""
	gen_exp_part $nom "A" "float" "$op" "class A{}"
	gen_exp_part $nom "A" "int" "$op" "class A{}"
	gen_exp_part $nom "A" "boolean" "$op" "class A{}"
	gen_exp_part $nom "int" "A" "$op" "class A{}"
	gen_exp_part $nom "float" "A" "$op" "class A{}"
	gen_exp_part $nom "float" "boolean" "$op" ""
	gen_exp_part $nom "int" "boolean" "$op" ""
	
}


gen_exp "eq" "=="
gen_exp "neq" "!="

