package martinetherton.domain



case class CurrencyExchangeRate(ticker: String,
                                bid: String,
                                ask: String,
                                open: String,
                                low: String,
                                high: String,
                                changes: Double,
                                date: String) {

}
