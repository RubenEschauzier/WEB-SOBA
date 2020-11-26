# WEB-SOBA
WEB-SOBA is a method for word embeddings-based semi-automatic ontology Building. The resulting ontology can then be used for aspect-based sentiment classification (ABSA).

## Instructions: 

### Building the ontology
This software first requires you to train word2vec vectors on a domain and then use this new word embedding model in the ontology builder. However if you want to use WEB_SOBA for the restaurant domain, you can download the required large files using the follow google drive: https://drive.google.com/open?id=19kkxN64GVWqnPKVcCy6a5JseRRb26usu. Add these files to a new folder called "largeData" in src/main/resources. The next step consists of semi-automatically building an ontology from the generated word embeddings. A small bit of user input is required in this ontology building step.

### Evaluating the ontology
The ontology obtained from WEB-SOBA can be used in aspect-based sentiment classification (ABSA). We recommend the following frameworks for evaluation: [Heracles](https://github.com/KSchouten/Heracles) and [HAABSA](https://github.com/ofwallaart/HAABSA) 

## Related Work: ##
Our method WEB-SOBA is related to the following papers:
-  Dera, E., Frasincar, F., Schouten, K., Zhuang, L.: Sasobus: Semi-automatic sentiment domain ontology building using synsets. In: European Semantic Web Conference. pp. 105–120. Springer (2020)
- Mikolov, T., Chen, K., Corrado, G., Dean, J.: Efficient estimation of word representations in vector space. 1st International Conference on Learning Representations
(ICLR 2013) (2013)
- Schouten, K., Frasincar, F.: Ontology-driven sentiment analysis of product and
service aspects. In: 15th Extended Semantic Web Conference (ESWC 2018). LNCS, vol. 10843, pp. 608–623. Springer (2018)
- Wallaart, O., Frasincar, F.: A hybrid approach for aspect-based sentiment analysis
using a lexicalized domain ontology and attentional neural models. In: 16th Extended Semantic Web Conference (ESWC 2019). LNCS, vol. 11503, pp. 363–378. Springer (2019)
- Zhuang, L., Schouten, K., Frasincar, F.: Soba: Semi-automated ontology builder
for aspect-based sentiment analysis. Journal of Web Semantics 60, 100–544 (2020)
