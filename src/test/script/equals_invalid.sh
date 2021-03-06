#! /bin/sh

# Auteur : lucie
# Version initiale : 12/01/2021

# Generation automatique des tests pour les opération binaires (+ / % * -)
cd "$(dirname "$0")"/../../.. || exit 1

PATH=./src/test/script/launchers:"$PATH"
chemin="./src/test/deca/context/invalid/created/object/"


gen_exp () {
	args1=$1
	args2=$2
	filename="$chemin/equals_${args1}_${args2}.deca"
	touch  $filename
	exec 3> $filename
	echo "// Description :" >&3
	echo "//    test equals entre un $args1 et un $args2" >&3
	echo "//" >&3
	echo "// Resultats :" >&3
	echo "//		Erreur contextuelle" >&3
	echo "//    	Ligne 11:  (3.71) Le membre gauche n'est pas de type 'class'" >&3
	echo "class A{}">&3
	echo "{" >&3
	echo "$args1 a;" >&3
	echo "$args2 b;" >&3
	echo "int c = a.equals(b);" >&3
	echo "}" >&3
	exec 3>&-
}



gen_exp "int" "A"
gen_exp "float" "A"
gen_exp "boolean" "A"
gen_exp "A" "float"
gen_exp "A" "boolean"
gen_exp "A" "int"

