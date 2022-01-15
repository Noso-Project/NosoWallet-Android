package com.s7evensoftware.nosowallet

const val NODE_TIMEOUT        = 1500
const val DELTA_TRIGGER       = true // ff false then nodes with Delta > 0 will be excluded from node selection
const val DEFAULT_SYNC_DELAY  = 10000L
const val MISSING_FUNDS       = "MISSING_FUNDS_ALERT"
const val QR_BITMAP_SIZE      = 512

const val NOSPath             = "NOSODATA" // directory
const val OptionsFileName     = "options.psk" //psk
const val ZipSumaryFileName   = "sumary.zip" //zip
const val SumaryDirectory     = "data" // directory
const val SumaryFileName      = "sumary.psk" //psk
const val SumaryFilePath      = "data/sumary.psk" // sumary path file
const val BotDataFilename     = "botdata.psk" // psk
const val NodeDataFilename    = "nodes.psk" // psk
const val NTPDataFilename     = "ntpservers.psk" // psk
const val WalletFilename      = "wallet.pkw" // pkw
const val GhostFilename       = "erased.pkw" // ghost wallet for erased addresses
const val WalletBackUpFile    = "wallet.pkw.bak" // pkw
const val UpdatesDirectory    = "UPDATES/" // directory
const val LogsDirectory       = "LOGS" // directory
const val LogsFilename        = "error_log.txt" // directory


const val B58Alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
const val B36Alphabet = "0123456789abcdefghijklmnopqrstuvwxyz"
const val B64Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

const val CoinSimbol = "NOSO"              // Coin 3 chars
const val CoinName = "Noso"                // Coin name
const val CoinChar = "N"                   // Char for addresses

const val InitialReward     = 5000000000   // Initial reward
const val ComisionCustom    = 200000       // 0.05 % of the Initial reward
const val CustomizationFee  = InitialReward/ComisionCustom
const val Comisiontrfr      = 10000        // Amount/Comisiontrfr = 0.01% of the amount
const val MinimunFee        = 10L          // Minimun fee for transfer
const val Protocol          = 1
const val ProgramVersion    = "1.0"


//Menu actions
const val DELETE_ADDRESS    = "MENU_DELETE_ADDRESS"
const val CUSTOMIZE_ADDRESS    = "MENU_CUSTOMIZE_ADDRESS"


