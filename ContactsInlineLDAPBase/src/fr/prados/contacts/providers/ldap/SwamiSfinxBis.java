package fr.prados.contacts.providers.ldap;

import java.util.Vector;

/**
 *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
 *
 *  Class name:   SwamiSfinxBis
 *
 *  Copyright:    Copyright (c) 2009
 *  Company:      Uppsala universitet
 *  @author       Allan Sjöö
 *  @version      1.0
 *
 *  Description:
 *  ============
 *
 *  Algoritm för SfinxBis
 *  Att koda ett namn med hjälp SfinxBis sker på följande sätt:
 *
 *  1) Konvertera hela namnet till versaler.
 *
 *  2) Om namnet börjar med en adelstitel avlägsna denna helt.
 *  a. af
 *  b. av
 *  c. da
 *  d. de
 *  e. de la
 *  f. de las
 *  g. de los
 *  h. del
 *  i. den
 *  j. des
 *  k. di
 *  l. do
 *  m. don
 *  n. dos
 *  o. du
 *  p. e
 *  q. in
 *  r. la
 *  s. le
 *  t. mac
 *  u. mc
 *  v. van
 *  w. van de
 *  x. van den
 *  y. van der
 *  z. von
 *  å. von dem
 *  ä. von der
 *  ö. y
 *  aa. S:t
 *
 *  3) Ta bort dubbelteckning i början av namnet.
 *
 *  4) Utför vissa försvenskningstransformationer i namnet.
 *  a. ”STIERN” -> ”STJÄRN”
 *  b. ”HIE” -> ”HJ”
 *  c. ”SIÖ” -> ”SJÖ”
 *  d. ”SCH” -> ”SH”
 *  e. ”QU” -> ”KV”
 *  f. ”IO” -> ”JO”
 *  g. ”PH” -> “F”
 *  h. Vokal + ”Ü” -> vokal + ”J”
 *  i. Vokal + ”Y” -> vokal + ”J”
 *  j. Vokal + ”I” -> vokal + ”J”
 *  k. ”H” + konsonant -> konsonant
 *  l. ”W” -> “V”
 *  m. ”Z” -> ”S”
 *  n. ”À”, ”Á”, ”Â”, ”Ã” -> ”A”
 *  o. ”Æ” -> ”Ä”
 *  p. ”Ç” -> ”C”
 *  q. ”È”, ”É”, ”Ê”, ”Ë” -> ”E”
 *  r. ”Ì”, ”Í”, ”Î”, ”Ï” -> ”I”
 *  s. ”Ð” -> ”ETH”
 *  t. ”Ñ” -> ”N”
 *  u. ”Ò”, ”Ó”, ”Ô”, ”Õ” -> ”O”
 *  v. ”Ø” -> ”Ö”
 *  w. ”Ù”, ”Ú”, ”Û” -> ”U”
 *  x. ”Ü” -> ”Y”
 *  y. ”Ý” -> ”Y”
 *  z. ”Þ” -> ”TH”
 *  å. ”ß” -> ”SS”
 *  ä. X -> X (övriga tecken beskriver själva sitt uttal)
 *
 *  5) Ta bort alla tecken som inte är ”A” – ”Ö”
 *
 *  6) Koda första ljudet (tecken som beskrivs som + tecken är ljudbestämmare
 *     och ingår inte i ljudet):
 *  a. Vokal -> ”$”
 *  b. ”DJ”, ”GJ”, ”HJ”, ”LJ” -> ”J”
 *  c. ”G” + mjuk vokal -> ”J” + mjuk vokal
 *  d. ”Q” -> ”K”
 *  e. ”C” + hård vokal -> ”K” + hård vokal
 *  f. ”C” + konsonant -> ”K” + konsonant
 *  g. ”X” -> ”S”
 *  h. ”C” + mjuk vokal -> ”S” + mjuk vokal
 *  i. “SKJ”, “STJ”, ”SCH”, ”SH”, “KJ”, ”TJ”, ”SJ” -> ”#”
 *  j. ”CH” + vokal, “SK” + mjuk vokal, ”K” + mjuk vokal -> ”#” + vokal
 *  k. X -> X (övriga tecken beskriver själva sitt uttal)
 *
 *  7) Dela upp namnet i två delar, första ljudet (se ovan) och resten.
 *
 *  8) Utför fonetiska transformationer i resten
 *     (för att ej förlora viktiga konsonantljud):
 *  a. ”DT” -> ”T”
 *  b. ”X” -> ”KS”
 *  c. X -> X (övriga tecken beskriver själva sitt uttal)
 *
 *  9) Koda resten till en sifferkod (bokstäver som uttalas lika ger upphov
 *     till samma kod, tecken som beskrivs som + tecken är ljudbestämmare och
 *     ingår inte i ljudet):
 *  a. ”B”, ”P” -> ”1”
 *  b. ”C” + hård vokal, ”C” + konsonant, ”C” som sista tecken,
 *     ”K”, ”G”, ”Q”, ”J” -> ”2”
 *  c. ”D”, ”T” -> ”3”
 *  d. ”L” -> ”4”
 *  e. ”M”, ”N” -> ”5”
 *  f. ”R” -> ”6”
 *  g. ”F”, ”V” -> ”7”
 *  h. ”C” + mjuk vokal, ”S”, ”Z” -> ”8”
 *  i. Vokal, “H” -> ”9”
 *
 *  10) Se till så att inga intilliggande dubbletter förekommer i den kodade resten.
 *
 *  11) Ta bort alla ”9” (vokaler och ”H”).
 *
 *  12) Sätt ihop de bägge delarna igen.
 *
 *  Methods: (name, description)
 *  ======== -------------------
 *
 *  codeSfinx - Kodning av efternamn till Svenskt Fonetiskt INDex
 *
 *  spliteName - Uppdelning av efternamn i flera delar
 *
 *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
 */


public class SwamiSfinxBis {

  /**
   *  Konstruktor==initieringsmetod.
   */
  public SwamiSfinxBis() {
  }

//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------

  /**
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   *
   *  Method name: splitName
   *
   *  Description:
   *  ============
   *
   *  Uppdelning av efternamn i flera delar
   *  Returnera en vektor med alla efternamnen
   *
   *  Vector nlista = new Vector()
   *  nlista = sf.splitName(name);
   *  for (int i = 0; i < nlista.size(); i++) {
   *     String tmp = (String) nlista.get(i);
   *  }
   *
   *  @param name dubbla efternamn
   *
   *  @return Vector med alla efternamnen
   *
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   */

  public Vector<String> splitName(String name) {
    Vector<String> nlista = new Vector<String>();
    String dn[];
    String tmp;
    int d, i;

    // Splitta namn i flera delar
    dn = name.split("[ -]");

    d = dn.length;
    if (d > 0) { // minst ett namn
      if (d > 1) { // fler än ett namn
        tmp = "";
        for (i = 0; i < d; i++) {
          if (checkAdelsprefix(dn[i])) { // kolla adelsprefix
            tmp = tmp + " " + dn[i];
          }
          else {
            tmp = tmp + " " + dn[i];
            nlista.add(tmp.trim());
            tmp = "";
          }
        }
      }
      else { // ett namn
        nlista.add(dn[0]);
      }
    }
    
    return nlista;
  }

  /**
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   *
   *  Method name: codeSfinx
   *
   *  Description:
   *  ============
   *
   *  Kodning av efternamn till Svenskt Fonetiskt INDex
   *
   *  @param name efternamn
   *
   *  @return kodat efternamn
   *
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   */

  public String codeSfinx(String name) {
    String str;
    int tkn;

    str = name.trim();

    if (str.length() != 0) { // ej tom sträng
      // Steg 1, Versaler
      str = str.toUpperCase();

      // Steg 2, Ta bort adelsprefix
      str = delAdelsprefix(str);

      // Steg 3, Ta bort dubbelteckning i början på namnet
      tkn = str.length();
      if (tkn >= 2) {
        if (str.substring(0, 1).compareTo(str.substring(1, 2)) == 0) {
          str = str.substring(1, tkn);
        }
      }

      // Steg 4, Försvenskning
      str = toSwedish(str);

      // Steg 5, Ta bort alla tecken som inte är A-Ö (65-90,196,197,214)
      str = delAO(str);

      //System.out.println(str);

      // Steg 6-12, Koda namnet
      if (str.length() != 0) { // ej tom sträng
        str = codeName(str);
      }
    }

    return str;
  }

//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------

  /**
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   *
   *  Method name: checkAdelsprefix
   *
   *  Description:
   *  ============
   *
   *  Kolla adelstrefix
   *
   *  @param name
   *
   *  @return
   *
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   */

  private boolean checkAdelsprefix(String adel) {
    boolean ok = false;

    adel = adel.toUpperCase().trim();

    if (adel.compareTo("AF") == 0) {
      ok = true;
    }
    else if (adel.compareTo("AV") == 0) {
      ok = true;
    }
    else if (adel.compareTo("DA") == 0) {
      ok = true;
    }
    else if (adel.compareTo("DE") == 0) {
      ok = true;
    }
    else if (adel.compareTo("DEL") == 0) {
      ok = true;
    }
    else if (adel.compareTo("DEM") == 0) {
      ok = true;
    }
    else if (adel.compareTo("DEN") == 0) {
      ok = true;
    }
    else if (adel.compareTo("DER") == 0) {
      ok = true;
    }
    else if (adel.compareTo("DES") == 0) {
      ok = true;
    }
    else if (adel.compareTo("DI") == 0) {
      ok = true;
    }
    else if (adel.compareTo("DO") == 0) {
      ok = true;
    }
    else if (adel.compareTo("DON") == 0) {
      ok = true;
    }
    else if (adel.compareTo("DOS") == 0) {
      ok = true;
    }
    else if (adel.compareTo("DU") == 0) {
      ok = true;
    }
    else if (adel.compareTo("E") == 0) {
      ok = true;
    }
    else if (adel.compareTo("IN") == 0) {
      ok = true;
    }
    else if (adel.compareTo("LA") == 0) {
      ok = true;
    }
    else if (adel.compareTo("LAS") == 0) {
      ok = true;
    }
    else if (adel.compareTo("LOS") == 0) {
      ok = true;
    }
    else if (adel.compareTo("LE") == 0) {
      ok = true;
    }
    else if (adel.compareTo("MAC") == 0) {
      ok = true;
    }
    else if (adel.compareTo("MC") == 0) {
      ok = true;
    }
    else if (adel.compareTo("S:T") == 0) {
      ok = true;
    }
    else if (adel.compareTo("VAN") == 0) {
      ok = true;
    }
    else if (adel.compareTo("VON") == 0) {
      ok = true;
    }
    else if (adel.compareTo("Y") == 0) {
      ok = true;
    }

    return ok;
  }

  /**
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   *
   *  Method name: delAdelsprefix
   *
   *  Description:
   *  ============
   *
   *  Om namnet börjar med en adelstitel avlägsna denna helt
   *
   *  @param name
   *
   *  @return
   *
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   */

  private String delAdelsprefix(String str) {
    String adel, org;
    int pos, tkn;
    boolean ok;

    org = str;
    pos = str.indexOf(" "); // första blanktecknet

    if ( (str.indexOf("VAN DEN ") != -1) || (str.indexOf("VAN DER ") != -1) ||
        (str.indexOf("VON DEM ") != -1) || (str.indexOf("VON DER ") != -1)) {
      pos = 7;
    }
    if ( (str.indexOf("DE LAS ") != -1) || (str.indexOf("DE LOS ") != -1) ||
        (str.indexOf("VAN DE ") != -1) || (str.indexOf("DER DE ") != -1)) {
      pos = 6;
    }
    if ( (str.indexOf("DE LA") != -1) || (str.indexOf("IN DE ") != -1)) {
      pos = 5;
    }

    if (pos != -1) {
      adel = str.substring(0, pos + 1);
      ok = false;

      if (adel.compareTo("AF ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("AV ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("DA ") == 0) {
        ok = true;
        //} else if (adel.compareTo("DAS ") == 0) {
        //  ok = true;
      }
      else if (adel.compareTo("DE ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("DE LA ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("DE LAS ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("DE LOS ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("DEL ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("DEN ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("DER DE ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("DES ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("DI ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("DO ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("DON ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("DOS ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("DU ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("E ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("IN DE ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("LA ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("LE ") == 0) {
        ok = true;
        //} else if (adel.compareTo("LI ") == 0) {
        //  ok = true;
        //} else if (adel.compareTo("LO ") == 0) {
        //  ok = true;
      }
      else if (adel.compareTo("MAC ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("MC ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("S:T ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("VAN ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("VAN DE ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("VAN DEN ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("VAN DER ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("VON ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("VON DEM ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("VON DER ") == 0) {
        ok = true;
      }
      else if (adel.compareTo("Y ") == 0) {
        ok = true;
      }

      if (ok) { // ta bort adelsprefix
        tkn = str.length();
        str = str.substring(pos + 1, tkn);
      }
    }

    // ta bort adels-infix, behövs inte om namnen delas m.h.a. splitName
    /*str = replaceStr(str, " AF ", " ");
    str = replaceStr(str, " AV ", " ");
    str = replaceStr(str, " DE LA ", " ");
    str = replaceStr(str, " DE LAS ", " ");
    str = replaceStr(str, " DE LOS ", " ");
    str = replaceStr(str, " DER DE ", " ");
    str = replaceStr(str, " IN DE ", " ");
    str = replaceStr(str, " VAN DE ", " ");
    str = replaceStr(str, " VAN DEN ", " ");
    str = replaceStr(str, " VAN DER ", " ");
    str = replaceStr(str, " VON DEM ", " ");
    str = replaceStr(str, " VON DER ", " ");
    str = replaceStr(str, " DEL ", " ");
    str = replaceStr(str, " DEN ", " ");
    str = replaceStr(str, " DON ", " ");
    str = replaceStr(str, " DOS ", " ");
    str = replaceStr(str, " DA ", " ");
    str = replaceStr(str, " DE ", " ");
    str = replaceStr(str, " DI ", " ");
    str = replaceStr(str, " DO ", " ");
    str = replaceStr(str, " DU ", " ");
    str = replaceStr(str, " LA ", " ");
    str = replaceStr(str, " LE ", " ");
    str = replaceStr(str, " VAN ", " ");
    str = replaceStr(str, " VON ", " ");
    str = replaceStr(str, " E ", " ");
    str = replaceStr(str, " Y ", " ");*/

    if (str.length() == 0) { // hela namnet togs bort
      str = org;
    }

    return str;
  }

  /**
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   *
   *  Method name: toSwedish
   *
   *  Description:
   *  ============
   *
   *  Utför vissa försvenskningstransformationer i namnet
   *
   *  @param name
   *
   *  @return
   *
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   */

  private String toSwedish(String str) {
    String ch1, ch2, tmp;
    int i, tkn;

    //str = replaceStr(str, "A:SON", "ANDERSSON");
    str = replaceStr(str, "STIERN", "STJÄRN");
    str = replaceStr(str, "HIE", "HJ");
    str = replaceStr(str, "SIÖ", "SJÖ");
    str = replaceStr(str, "SCH", "SH");
    str = replaceStr(str, "QU", "KV");
    str = replaceStr(str, "IO", "JO");
    str = replaceStr(str, "PH", "F");

    // vokal + "Ü", "Y" eller "I" -> vokal + "J"
    tkn = str.length();
    if (tkn >= 2) {
      for (i = 0; i < tkn - 1; i++) {
        ch1 = str.substring(i, i + 1);
        ch2 = str.substring(i + 1, i + 2);
        if (hardVokal(ch1) || softVokal(ch1)) {
          if ( (ch2.compareTo("Ü") == 0) || (ch2.compareTo("Y") == 0) ||
              (ch2.compareTo("I") == 0)) {
            str = str.substring(0, i + 1) + "J" + str.substring(i + 2, tkn);
          }
        }
      }
    }

    // "H" + konsonant -> konsonant
    tkn = str.length();
    if (tkn >= 2) {
      tmp = "";
      for (i = 0; i < tkn - 1; i++) {
        ch1 = str.substring(i, i + 1);
        ch2 = str.substring(i + 1, i + 2);
        if (ch1.compareTo("H") == 0 && consonant(ch2)) {
        }
        else {
          tmp = tmp + str.substring(i, i + 1);
        }
      }
      str = tmp + str.substring(tkn - 1, tkn); // lägg till sista tecknet
    }

    str = replaceStr(str, "W", "V");
    str = replaceStr(str, "Z", "S");

    str = replaceStr(str, "À", "A"); // 192
    str = replaceStr(str, "Á", "A"); // 193
    str = replaceStr(str, "Â", "A"); // 194
    str = replaceStr(str, "Ã", "A"); // 195

    str = replaceStr(str, "Æ", "Ä"); // 198, Æ->Ä
    str = replaceStr(str, "Ç", "C"); // 199, Ç->C

    str = replaceStr(str, "È", "E"); // 200
    str = replaceStr(str, "É", "E"); // 201
    str = replaceStr(str, "Ê", "E"); // 202
    str = replaceStr(str, "Ë", "E"); // 203

    str = replaceStr(str, "Ì", "I"); // 204
    str = replaceStr(str, "Í", "I"); // 205
    str = replaceStr(str, "Î", "I"); // 206
    str = replaceStr(str, "Ï", "I"); // 207

    str = replaceStr(str, "Ð", "ETH"); // 208, Ð->ETH
    str = replaceStr(str, "Ñ", "N"); // 209, Ñ->N

    str = replaceStr(str, "Ò", "O"); // 210
    str = replaceStr(str, "Ó", "O"); // 211
    str = replaceStr(str, "Ô", "O"); // 212
    str = replaceStr(str, "Õ", "O"); // 213

    str = replaceStr(str, "Ø", "Ö"); // 216, Ø->Ö

    str = replaceStr(str, "Ù", "U"); // 217
    str = replaceStr(str, "Ú", "U"); // 218
    str = replaceStr(str, "Û", "U"); // 219

    str = replaceStr(str, "Ü", "Y"); // 220, Ü->Y
    str = replaceStr(str, "Ý", "Y"); // 221, Ý->Y
    str = replaceStr(str, "Þ", "TH"); // 222, Þ->TH
    str = replaceStr(str, "ß", "SS"); // 223, ß->SS

    return str;
  }

  /**
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   *
   *  Method name: delAO
   *
   *  Description:
   *  ============
   *
   *  Ta bort alla tecken som inte är ”A” – ”Ö”
   *
   *  @param name
   *
   *  @return
   *
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   */

  private String delAO(String str) {
    String tmp;
    int c, i, tkn;

    tkn = str.length();
    tmp = "";
    for (i = 0; i < tkn; i++) {
      c = (int) str.charAt(i);
      if ( (c >= 65 && c <= 90) || (c == 196) || (c == 197) || (c == 214)) {
        tmp = tmp + str.substring(i, i + 1);
      }
    }
    str = tmp;

    return str;
  }

  /**
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   *
   *  Method name: codeName
   *
   *  Description:
   *  ============
   *
   *  Koda efternamnet
   *
   *  @param name
   *
   *  @return
   *
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   */

  private String codeName(String str) {
    String ch1, ch2, ch3, kv;
    String sf1 = "";
    String sf2 = "";
    int pos, tkn;


    // Steg 6, Koda första ljudet
    tkn = str.length();
    if (tkn >= 1) {
      ch1 = str.substring(0, 1);
    } else {
      ch1 = " ";
    }

    if (tkn >= 2) {
      ch2 = str.substring(0, 2);
      kv = str.substring(1, 2);
    } else {
      ch2 = "  ";
      kv = "  ";
    }

    if (tkn >= 3) {
      ch3 = str.substring(0, 3);
    } else {
      ch3 = "   ";
    }

    if (hardVokal(ch1) || softVokal(ch1)) {
      sf1 = "$";
      pos = 1;
    }
    else if (ch1.compareTo("Q") == 0) {
      sf1 = "K";
      pos = 1;
    }
    else if (ch1.compareTo("X") == 0) {
      sf1 = "S";
      pos = 1;
    }
    else if (ch1.compareTo("G") == 0 && softVokal(kv)) {
      sf1 = "J";
      pos = 1;
    }
    else if (ch1.compareTo("C") == 0 && softVokal(kv)) {
      sf1 = "S";
      pos = 1;
    }
    else if (ch1.compareTo("C") == 0 && hardVokal(kv)) {
      sf1 = "K";
      pos = 1;
    }
    else if (ch1.compareTo("C") == 0 && consonant(kv)) {
      sf1 = "K";
      pos = 1;
    }
    else if (ch1.compareTo("K") == 0 && softVokal(kv)) {
      sf1 = "#";
      pos = 1;
    }
    else if (ch1.compareTo("SK") == 0 && softVokal(kv)) {
      sf1 = "#";
      pos = 2;
    }
    else if (ch2.compareTo("CH") == 0 && (softVokal(kv) || hardVokal(kv))) {
      sf1 = "#";
      pos = 2;
    }
    else if ( (ch2.compareTo("DJ") == 0) || (ch2.compareTo("GJ") == 0) ||
             (ch2.compareTo("HJ") == 0) || (ch2.compareTo("LJ") == 0)) {
      sf1 = "J";
      pos = 2;
    }
    else if ( (ch2.compareTo("SH") == 0) || (ch2.compareTo("KJ") == 0) ||
             (ch2.compareTo("SJ") == 0) || (ch2.compareTo("TJ") == 0)) {
      sf1 = "#";
      pos = 2;
    }
    else if ( (ch3.compareTo("SKJ") == 0) || (ch3.compareTo("STJ") == 0) ||
             (ch3.compareTo("SCH") == 0)) {
      sf1 = "#";
      pos = 3;
    }
    else { // övriga tecken
      sf1 = ch1;
      pos = 1;
    }

    //System.out.println(str + " " + sf1);

    // Steg 7, Dela upp namnet i två delar
    tkn = str.length();
    if (tkn >= 1) {
      sf2 = str.substring(pos, tkn);
    }

    // Steg 8, Utför fonetisk transformation i resten
    //sf2 = replaceStr(sf2, "CH", "S");
    sf2 = replaceStr(sf2, "DT", "T");
    sf2 = replaceStr(sf2, "X", "KS");

    // Steg 9, Koda resten till en sifferkod
    sf2 = codeNumbers(sf2);

    // Steg 10, Ta bort intilliggande dubbletter
    sf2 = delDuplicate(sf2);

    // Steg 11, Ta bort alla "9"
    sf2 = replaceStr(sf2, "9", "");

    //System.out.println(str + "->" + sf1 + sf2);

    // Steg 12, Sätt ihop delarna igen
    str = sf1 + sf2;

    return str;
  }

  /**
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   *
   *  Method name: codeNumbers
   *
   *  Description:
   *  ============
   *
   *  Koda resten till en sifferkod
   *
   *  @param name
   *
   *  @return
   *
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   */

  private String codeNumbers(String sf2) {
    String ch1, ch2, tmp;
    int i, tkn;

    tkn = sf2.length();
    if (tkn >= 2) {
      tmp = "";
      for (i = 0; i < tkn - 1; i++) {
        ch1 = sf2.substring(i, i + 1);
        ch2 = sf2.substring(i + 1, i + 2);
        if (ch1.compareTo("C") == 0) {
          if (hardVokal(ch2) || consonant(ch2)) { // C + hård vokal, konsonant
            tmp = tmp + "2";
          }
          else { // C + mjuk vokal
            tmp = tmp + "8";
          }
        }
        else { // övriga tecken
          tmp = tmp + ch1;
        }
      }
      sf2 = tmp + sf2.substring(tkn - 1, tkn); // lägg till sista tecknet
    }

    // Vokal -> "9"
    for (i = 0; i < tkn; i++) {
      ch1 = sf2.substring(i, i + 1);
      if (hardVokal(ch1) || softVokal(ch1)) {
        sf2 = replaceStr(sf2, ch1, "9");
      }
    }

    sf2 = replaceStr(sf2, "B", "1");
    sf2 = replaceStr(sf2, "P", "1");
    sf2 = replaceStr(sf2, "G", "2");
    sf2 = replaceStr(sf2, "J", "2");
    sf2 = replaceStr(sf2, "C", "2"); // nytt?
    sf2 = replaceStr(sf2, "K", "2");
    sf2 = replaceStr(sf2, "Q", "2");
    sf2 = replaceStr(sf2, "D", "3");
    sf2 = replaceStr(sf2, "T", "3");
    sf2 = replaceStr(sf2, "L", "4");
    sf2 = replaceStr(sf2, "M", "5");
    sf2 = replaceStr(sf2, "N", "5");
    sf2 = replaceStr(sf2, "R", "6");
    sf2 = replaceStr(sf2, "F", "7");
    sf2 = replaceStr(sf2, "V", "7");
    sf2 = replaceStr(sf2, "S", "8");
    sf2 = replaceStr(sf2, "Z", "8");
    sf2 = replaceStr(sf2, "H", "9");

    return sf2;
  }

  /**
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   *
   *  Method name: delDuplicate
   *
   *  Description:
   *  ============
   *
   *  Ta bort intilliggande dubbletter
   *
   *  @param name
   *
   *  @return
   *
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   */

  private String delDuplicate(String sf2) {
    String ch1, ch2, tmp;
    int i, tkn;

    tkn = sf2.length();
    if (tkn >= 2) {
      tmp = sf2.substring(0, 1);
      for (i = 0; i < tkn - 1; i++) {
        ch1 = sf2.substring(i, i + 1);
        ch2 = sf2.substring(i + 1, i + 2);
        if (ch1.compareTo(ch2) != 0) {
          tmp = tmp + ch2;
         }
      }
      sf2 = tmp;
    }

    return sf2;
  }

  /**
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   *
   *  Method name: replaceStr
   *
   *  Description:
   *  ============
   *
   *  Byt tecken i en sträng
   *
   *  @param name
   *
   *  @return
   *
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   */

  private String replaceStr(String str, String gstr, String nstr) {
    if (str.indexOf(gstr) != -1) {
      str = str.replaceAll(gstr, nstr); // gstr->nstr
    }

    return str;
  }

  /**
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   *
   *  Method name: consonant
   *
   *  Description:
   *  ============
   *
   *  Kolla om det är en konsonant
   *
   *  @param name
   *
   *  @return
   *
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   */

  private boolean consonant(String ch) {
    int c;
    boolean ok;

    ok = false;
    c = (int) ch.charAt(0);
    if ( (c >= 66 && c <= 68) || (c >= 70 && c <= 72) || (c >= 74 && c <= 78) ||
        (c >= 80 && c <= 84) || (c >= 86 && c <= 88) || (c == 90)) {
      ok = true;
    }

    return ok;
  }

  /**
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   *
   *  Method name: hardVokal
   *
   *  Description:
   *  ============
   *
   *  Kolla om det är en hård vokal (A, O, U, Å)
   *
   *  @param name
   *
   *  @return
   *
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   */

  private boolean hardVokal(String ch) {
    int c;
    boolean ok;

    ok = false;
    c = (int) ch.charAt(0);
    if ( (c == 65) || (c == 79) || (c == 85) || (c == 197)) {
      ok = true;
    }

    return ok;
  }

  /**
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   *
   *  Method name: softVokal
   *
   *  Description:
   *  ============
   *
   *  Kolla om det är en mjuk vokal (E, I, Y, Ä, Ö)
   *
   *  @param name
   *
   *  @return
   *
   *--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*--*
   */

  private boolean softVokal(String ch) {
    int c;
    boolean ok;

    ok = false;
    c = (int) ch.charAt(0);
    if ( (c == 69) || (c == 73) || (c == 89) || (c == 196) || (c == 214)) {
      ok = true;
    }

    return ok;
  }

} // class