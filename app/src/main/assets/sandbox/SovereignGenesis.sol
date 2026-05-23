// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;
import "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import "@openzeppelin/contracts/token/ERC721/extensions/ERC721URIStorage.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract SovereignGenesis is ERC721, ERC721URIStorage, Ownable {
    uint256 private _nextTokenId;

    struct QuantumData {
        string quantumSignature;
        string assetName;
        uint256 timestamp;
    }

    mapping(uint256 => QuantumData) public quantumRecords;

    event QuantumNFTForged(
        uint256 indexed tokenId, 
        address indexed owner, 
        string quantumSignature
    );

    constructor(address initialOwner)
        ERC721("Sovereign Genesis", "SOV101")
        Ownable(initialOwner)
    {}

    function forgeNFT(
        address to,
        string memory uri,
        string memory _quantumSignature,
        string memory _assetName
    ) public returns (uint256) {
        uint256 tokenId = _nextTokenId++;
        _safeMint(to, tokenId);
        _setTokenURI(tokenId, uri);

        quantumRecords[tokenId] = QuantumData({
            quantumSignature: _quantumSignature,
            assetName: _assetName,
            timestamp: block.timestamp
        });

        emit QuantumNFTForged(tokenId, to, _quantumSignature);
        return tokenId;
    }

    function tokenURI(uint256 tokenId)
        public view override(ERC721, ERC721URIStorage) returns (string memory)
    {
        return super.tokenURI(tokenId);
    }

    function supportsInterface(bytes4 interfaceId)
        public view override(ERC721, ERC721URIStorage) returns (bool)
    {
        return super.supportsInterface(interfaceId);
    }
}
